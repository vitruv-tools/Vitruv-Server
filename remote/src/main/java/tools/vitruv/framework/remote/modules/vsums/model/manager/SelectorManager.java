package tools.vitruv.framework.remote.modules.vsums.model.manager;

import org.springframework.stereotype.Component;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.SelectorWrapper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle, retrieval, and storage of {@link SelectorWrapper} instances.
 * This class acts as a centralized manager for selectors, providing operations
 * for adding, retrieving, and removing selectors, while ensuring thread-safe
 * access and manipulation of the underlying data.
 * <p>
 * The {@link SelectorWrapper} instances encapsulate information about a specific
 * selector, allowing integration with related components and resources.
 */
@Component
public class SelectorManager {
  private final Map<UUID, SelectorWrapper> selectors = new ConcurrentHashMap<>();

  /**
   * Retrieves a {@link SelectorWrapper} associated with the specified selector identifier.
   * This method fetches the {@link SelectorWrapper} from the internal cache if available.
   *
   * @param selectorId the unique identifier of the selector to retrieve
   * @return the {@link SelectorWrapper} associated with the given selector identifier,
   * or {@code null} if no such selector exists in the cache
   */
  public SelectorWrapper getSelector(UUID selectorId) {
    return selectors.get(selectorId);
  }

  /**
   * Adds a {@link SelectorWrapper} to the internal cache. The {@link SelectorWrapper} is stored
   * using its unique identifier as the key. If a {@link SelectorWrapper} with the same identifier
   * is already present, it will be replaced.
   *
   * @param selectorWrapper the {@link SelectorWrapper} to add to the internal cache. It must
   *                        contain a valid unique identifier retrievable via {@link SelectorWrapper#selectorId()}.
   */
  public void addSelector(SelectorWrapper selectorWrapper) {
    selectors.put(selectorWrapper.selectorId(), selectorWrapper);
  }

  /**
   * Removes a selector associated with the specified unique identifier from the internal cache.
   * The operation will silently return if no selector is found for the given identifier.
   *
   * @param selectorId the unique identifier of the selector to be removed
   */
  public void removeSelector(UUID selectorId) {
    selectors.remove(selectorId);
  }
}
