package tools.vitruv.framework.remote.modules.vsums.model.entities;

import tools.vitruv.framework.remote.common.entities.BaseRepo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpenInconsistencyRepo extends BaseRepo<OpenInconsistency> {

    List<OpenInconsistency> findByStateOrderByCreatedAtDesc(OpenInconsistencyState state);

    List<OpenInconsistency> findAllByOrderByCreatedAtDesc();

    Optional<OpenInconsistency> findByVsumIdAndState(UUID vsumId, OpenInconsistencyState state);

    Optional<OpenInconsistency> findByTaskIdAndState(UUID taskId, OpenInconsistencyState state);

    Optional<OpenInconsistency> findByTaskId(UUID taskId);
}
