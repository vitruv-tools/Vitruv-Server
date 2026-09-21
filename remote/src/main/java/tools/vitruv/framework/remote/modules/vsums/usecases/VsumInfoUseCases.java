package tools.vitruv.framework.remote.modules.vsums.usecases;

import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfoRepo;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.CreateVsumRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.UpdateVsumInfoRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.VsumInfoResponseBody;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VsumInfoUseCases {
    private final VsumInfoRepo vsumInfoRepo;

    public VsumInfoResponseBody[] getVsumInfos() {
        return vsumInfoRepo.findAll().stream()
                .map(vsumInfo -> new VsumInfoResponseBody(
                        vsumInfo.getId(),
                        vsumInfo.getMetaModelName(),
                        vsumInfo.getName(),
                        vsumInfo.getDescription()
                ))
                .toArray(VsumInfoResponseBody[]::new);
    }

    public VsumInfoResponseBody createVsumInfo(CreateVsumRequestBody body) {
        VsumInfo info = new VsumInfo(body.metamodelName(), body.name(), body.description());
        return saveVsumInfoAndReturnBody(info);
    }

    public VsumInfoResponseBody updateVsumInfo(UUID vsumId, UpdateVsumInfoRequestBody body) {
        VsumInfo info = vsumInfoRepo.findById(vsumId)
                .orElseThrow(() -> new IllegalArgumentException("Vsum not found: " + vsumId));

        info.setName(body.name());
        info.setDescription(body.description());

        return saveVsumInfoAndReturnBody(info);
    }

    private VsumInfoResponseBody saveVsumInfoAndReturnBody(VsumInfo info) {
        VsumInfo savedInfo = vsumInfoRepo.save(info);
        return new VsumInfoResponseBody(
                savedInfo.getId(),
                savedInfo.getMetaModelName(),
                savedInfo.getName(),
                savedInfo.getDescription()
        );
    }
}
