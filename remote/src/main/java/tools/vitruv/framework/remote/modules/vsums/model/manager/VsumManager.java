package tools.vitruv.framework.remote.modules.vsums.model.manager;

import tools.vitruv.framework.remote.common.utils.json.JsonMapper;
import tools.vitruv.framework.remote.config.VsumProperties;
import tools.vitruv.framework.remote.modules.vsums.async.VitruviusInteractionResultProvider;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyState;
import tools.vitruv.framework.remote.modules.vsums.model.entities.ViewUpdateRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfoRepo;
import tools.vitruv.framework.remote.modules.vsums.model.services.ViewService;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.VsumWrapper;
import tools.vitruv.framework.remote.vsumprovider.VsumProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import tools.vitruv.framework.vsum.VirtualModel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the retrieval, initialization, caching, and deletion of VSUM instances.
 * VSUMs (Virtualized Service Utility Models) are dynamically loaded using metadata
 * and stored in a virtual environment for further processing or usage.
 * This class interacts with the underlying repository and provider manager to ensure
 * efficient management of VSUM resources.
 * <p>
 * The manager uses an internal cache to store already initialized VSUMs, reducing
 * redundant operations. The cache is updated or invalidated as necessary when
 * updates or deletions occur.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VsumManager {

    private final VsumProperties vsumProperties;
    private final VsumProviderManager vsumProviderManager;
    private final VsumInfoRepo vsumInfoRepo;
    private final ViewUpdateRepo viewUpdateRepo;
    private final ViewService viewService;
    private final ViewManager viewManager;
    private final OpenInconsistencyRepo openInconsistencyRepo;
    private final VitruviusInteractionResultProvider interactionResultProvider;

    private final Map<UUID, VsumWrapper> vsums = new ConcurrentHashMap<>();

    /**
     * Retrieves a {@link VsumWrapper} associated with the specified VSUM identifier.
     * If the VSUM is already cached, it is returned directly. Otherwise, it initializes
     * and loads the VSUM using metadata, the associated virtual model, and a JSON mapper,
     * then caches the result before returning it.
     *
     * @param vsumId the unique identifier of the VSUM to retrieve
     * @return the {@link VsumWrapper} for the specified identifier
     * @throws NoSuchElementException if the VSUM metadata cannot be found in the repository
     */
    public VsumWrapper getVsum(UUID vsumId) {
        val vsumWrapper = vsums.get(vsumId);

        if (vsumWrapper != null) {
            return vsumWrapper;
        }

        val info = vsumInfoRepo.findById(vsumId).orElseThrow();
        val provider = vsumProviderManager.getVsumProvider(info.getMetaModelName());
        val storagePath = getVsumStoragePath(info);
        repairStorageIfNeeded(info, storagePath);
        val virtualModel = initializeVirtualModel(provider, info, storagePath);
        val jsonMapper = new JsonMapper(virtualModel.getFolder());

        val loadedVsumWrapper = new VsumWrapper(info, virtualModel, jsonMapper);
        replayLastUpdate(loadedVsumWrapper);

        vsums.put(vsumId, loadedVsumWrapper);

        return loadedVsumWrapper;
    }

    /**
     * Deletes the VSUM with the specified identifier. This method ensures that
     * any cached information about the VSUM is removed and the corresponding
     * entry in the repository is deleted.
     *
     * @param vsumId the unique identifier of the VSUM to delete
     */
    public void deleteVsum(UUID vsumId) {
        viewManager.removeAndCloseViewsForVsum(vsumId);
        openInconsistencyRepo.findByVsumIdAndState(vsumId, OpenInconsistencyState.OPEN).ifPresent(open -> {
            open.setState(OpenInconsistencyState.FAILED);
            open.setResolvedAt(Instant.now());
            open.setMessage("VSUM deleted while inconsistency was still open");
            openInconsistencyRepo.save(open);
        });
        unloadVsum(vsumId);
        vsumInfoRepo.deleteById(vsumId);
    }

    /**
     * Closes open views and drops the in-memory VSUM so the next access reloads a clean recorder.
     * Used after failed/aborted propagation that can leave EMF change recording stuck.
     */
    public void evictVsum(UUID vsumId) {
        viewManager.removeAndCloseViewsForVsum(vsumId);
        unloadVsum(vsumId);
    }

    private void unloadVsum(UUID vsumId) {
        vsums.remove(vsumId);
    }

    private VirtualModel initializeVirtualModel(VsumProvider provider, VsumInfo info, Path storagePath) {
        try {
            return provider.getInitializer(storagePath, interactionResultProvider).init();
        } catch (RuntimeException e) {
            if (!VsumStorageHealth.isXmlParseFailure(e)) {
                throw e;
            }
            log.warn(
                    "VSUM {} storage at {} is not valid XMI ({}). Quarantining folder and retrying with a fresh path.",
                    info.getId(),
                    storagePath,
                    e.getMessage());
            Path retryPath = quarantineAndFreshPath(info, storagePath);
            return provider.getInitializer(retryPath, interactionResultProvider).init();
        }
    }

    private void repairStorageIfNeeded(VsumInfo info, Path storagePath) {
        if (!VsumStorageHealth.repairIncompleteModelPair(storagePath)) {
            return;
        }
        viewUpdateRepo.deleteByVsumInfoId(info.getId());
        log.warn(
                "Repaired incomplete example.model / example.model2 pair for VSUM {} at {}. "
                        + "Cleared stored view snapshots so a clean model can load.",
                info.getId(),
                storagePath);
    }

    private void replayLastUpdate(VsumWrapper loadedVsumWrapper) {
        try {
            viewService.loadLastUpdate(loadedVsumWrapper);
        } catch (RuntimeException e) {
            if (VsumStorageHealth.isDanglingProxyFailure(e)) {
                viewUpdateRepo.deleteByVsumInfoId(loadedVsumWrapper.info().getId());
                log.warn(
                        "Dropped stale view snapshots for VSUM {} after dangling proxy during replay.",
                        loadedVsumWrapper.info().getId());
            }
            log.warn(
                    "Could not replay last view snapshot for VSUM {}. Continuing with recovered storage.",
                    loadedVsumWrapper.info().getId(),
                    e);
        }
    }

    private Path getVsumStoragePath(VsumInfo info) {
        try {
            return VsumStoragePathResolver.resolveReplacingCorrupt(
                    Path.of(vsumProperties.vsumStorageDir()),
                    info.getMetaModelName(),
                    info.getId());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to resolve VSUM storage path for " + info.getId(), e);
        }
    }

    private Path quarantineAndFreshPath(VsumInfo info, Path storagePath) {
        try {
            VsumStoragePathResolver.quarantine(storagePath);
            return Path.of(vsumProperties.vsumStorageDir())
                    .resolve(VsumStoragePathResolver.directoryPrefix(info.getMetaModelName(), info.getId()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to quarantine corrupt VSUM storage for " + info.getId(), e);
        }
    }
}
