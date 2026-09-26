package tools.vitruv.framework.remote.modules.vsums.model.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ViewUpdateRepo extends JpaRepository<ViewUpdate, UUID> {
  ViewUpdate findFirstByVsumInfoIdOrderByTimestampDesc(UUID vsumId);

  List<ViewUpdate> findByVsumInfoIdOrderByTimestampDesc(UUID vsumId);

  void deleteByVsumInfoId(UUID vsumId);
}
