package tools.vitruv.framework.remote.modules.vsums.model.entities;

import tools.vitruv.framework.remote.common.entities.BaseRepo;

import java.util.List;
import java.util.UUID;

public interface InconsistencyCommentRepo extends BaseRepo<InconsistencyComment> {

    List<InconsistencyComment> findByInconsistencyIdOrderByCreatedAtAsc(UUID inconsistencyId);
}
