package tools.vitruv.framework.remote.modules.vsums.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import tools.vitruv.framework.remote.common.entities.BaseEntity;

import java.time.Instant;
import java.util.List;

/**
 * Represents a record of updates or changes related to a specific view in the context of a VSUM.
 * This entity is persisted in the database and associates information about the view type, related objects,
 * and timestamps for tracking.
 * <p>
 * The entity tracks the following details:
 * - References a {@link VsumInfo} entity describing the related VSUM information.
 * - Stores the name of the view type.
 * - Maintains a list of object class names that are selected or associated with the view update.
 * - Encodes the state of the resource set at the time of the update as a text field.
 * - Captures the timestamp of when the update occurred.
 * <p>
 * The entity uses cascading deletion behavior to ensure that associated data is cleaned up when this
 * entity or related entities are removed from the database.
 * <p>
 * This class extends {@link BaseEntity}, inheriting an identifier and baseline behaviors.
 */
@Entity
@Table(name = "view-updates")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ViewUpdate extends BaseEntity {

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  private VsumInfo vsumInfo;

  private String viewTypeName;

  @ElementCollection
  @CollectionTable(
      name = "view_updates_selected_objects_eclasses",
      joinColumns = @JoinColumn(name = "view_update_id")
  )
  @Column(name = "selected_object_eclass_name")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private List<String> selectedObjectEClassNames;

  @Column(columnDefinition = "TEXT")
  private String encodedResourceSet;

  private Instant timestamp;
}
