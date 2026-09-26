package tools.vitruv.framework.remote.common.entities;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

/**
 * Abstract base class for all entities in the application that require a UUID identifier.
 * This class provides common behavior for entities, such as `equals` and `hashCode` implementations
 * based on the unique identifier.
 * <p>
 * The entity uses a UUID as the primary key, which is generated automatically.
 * It also supports inheritance with the `TABLE_PER_CLASS` strategy to share common properties
 * among derived entities while persisting them in separate database tables.
 * <p>
 * An entity must extend this class to utilize its structure and behavior.
 * <p>
 * An entity is considered equal to another entity if their UUID identifiers are the same.
 * The `hashCode` method is implemented using the UUID identifier. An exception will be thrown
 * if the identifier is `null` during hash code generation.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
public abstract class BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof BaseEntity)) return false;
    if (id == null) return false;
    return id.equals(((BaseEntity) other).id);
  }

  @Override
  public int hashCode() {
    if (id == null) {
      throw new IllegalStateException("Entity without id cannot be hashed.");
    }
    return id.hashCode();
  }

}
