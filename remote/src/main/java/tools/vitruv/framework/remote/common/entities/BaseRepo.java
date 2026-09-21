package tools.vitruv.framework.remote.common.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

/**
 * Base repository interface providing generic CRUD operations for entities
 * that extend {@link BaseEntity}. This interface should be extended by
 * specific repository interfaces to inherit standard Spring Data JPA
 * functionality.
 * <p>
 * The repository utilizes {@link UUID} as the primary key type for entities.
 * <p>
 * This interface is annotated with {@code @NoRepositoryBean}, meaning it will not
 * be instantiated directly as a Spring repository bean. Instead, it serves
 * as a base for other repository interfaces to extend.
 *
 * @param <T> the type of the entity managed by this repository, must extend {@link BaseEntity}
 */
@NoRepositoryBean
public interface BaseRepo<T extends BaseEntity> extends JpaRepository<T, UUID> {
}
