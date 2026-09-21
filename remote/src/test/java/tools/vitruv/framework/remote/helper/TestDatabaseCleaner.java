package tools.vitruv.framework.remote.helper;

import tools.vitruv.framework.remote.modules.vsums.model.entities.InconsistencyCommentRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.ViewUpdateRepo;
import tools.vitruv.framework.remote.modules.vsums.model.entities.VsumInfoRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestDatabaseCleaner {

    private final ViewUpdateRepo viewUpdateRepo;
    private final VsumInfoRepo vsumInfoRepo;
    private final InconsistencyCommentRepo inconsistencyCommentRepo;
    private final OpenInconsistencyRepo openInconsistencyRepo;

    public void cleanAll() {
        inconsistencyCommentRepo.deleteAll();
        openInconsistencyRepo.deleteAll();
        viewUpdateRepo.deleteAll();
        vsumInfoRepo.deleteAll();
    }
}
