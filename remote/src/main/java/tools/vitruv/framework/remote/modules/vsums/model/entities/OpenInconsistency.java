package tools.vitruv.framework.remote.modules.vsums.model.entities;

import tools.vitruv.framework.remote.common.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * At most one {@link OpenInconsistencyState#OPEN} row should exist per VSUM.
 * Created when the editor dismisses a waiting interaction; resolved when that task completes from the hub.
 */
@Entity
@Table(name = "open-inconsistencies")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OpenInconsistency extends BaseEntity {

    private UUID vsumId;

    private UUID taskId;

    private UUID viewId;

    private String vsumName;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String interactionJson;

    /** Resource set JSON captured when the inconsistency was parked (same format as POST /v1/views). */
    @Column(columnDefinition = "TEXT")
    private String modelSnapshotEncodedResourceSet;

    private String modelSnapshotViewTypeName;

    @Enumerated(EnumType.STRING)
    private OpenInconsistencyState state;

    private Instant createdAt;

    private Instant resolvedAt;

    private String resolvedBy;

    @Column(columnDefinition = "TEXT")
    private String resolutionChoice;

    @Column(columnDefinition = "TEXT")
    private String resolutionComment;
}
