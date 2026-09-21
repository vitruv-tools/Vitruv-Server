package tools.vitruv.framework.remote.modules.vsums.usecasesimpls;

import tools.vitruv.framework.remote.modules.vsums.model.manager.VsumManager;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.VsumWrapper;
import tools.vitruv.framework.remote.modules.vsums.usecases.strategies.VsumUseCasesStrategy;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;
import tools.vitruv.framework.views.ViewType;

import java.util.UUID;

/**
 * An implementation of the {@link VsumUseCasesStrategy} interface that provides
 * functionalities related to VSUM operations. This class interacts with the
 * {@link VsumManager} to manage the lifecycle of Virtualized Service Utility Models (VSUMs).
 * <p>
 * This implementation focuses on retrieving view types and supporting the deletion
 * of VSUM instances. It ensures that the associated VSUM data is managed effectively
 * via the {@link VsumManager}.
 * <p>
 * The primary responsibilities of this class include:
 * - Fetching view types associated with a VSUM.
 * - Deleting a VSUM instance.
 */
@Component
@RequiredArgsConstructor
public class InternalVsumStrategyImpl implements VsumUseCasesStrategy {
    private final VsumManager vsumManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getViewTypes(UUID vsumId) {
        val vsumWrapper = getVsumOrThrow(vsumId);
        return vsumWrapper.virtualModel().getViewTypes().stream()
                .map(ViewType::getName)
                .sorted()
                .toArray(String[]::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVsum(UUID vsumId) {
        vsumManager.deleteVsum(vsumId);
    }

    private VsumWrapper getVsumOrThrow(UUID viewId) {
        val vsumWrapper = vsumManager.getVsum(viewId);
        if (vsumWrapper == null) {
            throw new IllegalArgumentException("Vsum not found: " + viewId);
        }
        return vsumWrapper;
    }
}
