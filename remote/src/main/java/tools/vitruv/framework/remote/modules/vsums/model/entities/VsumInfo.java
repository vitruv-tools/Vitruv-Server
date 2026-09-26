package tools.vitruv.framework.remote.modules.vsums.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.vitruv.framework.remote.common.entities.BaseEntity;

/**
 * Represents metadata information for a VSUM entity within the system.
 * This entity is persisted in the database and is associated with additional
 * details about the VSUM, including its meta-model name, display name,
 * and description.
 * <p>
 * The entity includes the following attributes:
 * - `metaModelName`: Specifies the name of the meta-model associated with the VSUM.
 * - `name`: Represents the name or title of the VSUM.
 * - `description`: Provides an optional textual description of the VSUM's purpose or details.
 * <p>
 * This class extends {@link BaseEntity}, inheriting a UUID as its primary identifier
 * and common entity behaviors such as equality and hash code implementation.
 * <p>
 * The entity is mapped to a database table named `vsum-infos` and utilizes
 * standard JPA annotations for its fields and configuration. It also includes
 * Lombok annotations to reduce boilerplate for constructors and accessors.
 */
@Entity
@Table(name = "vsum-infos")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VsumInfo extends BaseEntity {
  private String metaModelName;

  private String name;

  private String description;
}
