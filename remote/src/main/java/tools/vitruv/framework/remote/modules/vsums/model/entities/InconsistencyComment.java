package tools.vitruv.framework.remote.modules.vsums.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.vitruv.framework.remote.common.entities.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inconsistency-comments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InconsistencyComment extends BaseEntity {

  private UUID inconsistencyId;

  private String author;

  @Column(columnDefinition = "TEXT")
  private String body;

  private Instant createdAt;
}
