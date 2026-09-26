package tools.vitruv.framework.remote.modules.vsums.model.manager;

import lombok.val;
import org.springframework.stereotype.Component;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.ViewWrapper;
import tools.vitruv.framework.views.View;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle, retrieval, and storage of {@link ViewWrapper} instances.
 * This class acts as a centralized manager for views, providing operations
 * for adding, retrieving, and removing views, while ensuring thread-safe
 * access and manipulation of the underlying data.
 * <p>
 * The {@link ViewWrapper} instances encapsulate information about a specific
 * view, allowing integration with related components and resources.
 */
@Component
public class ViewManager {
  private final Map<UUID, ViewWrapper> views = new ConcurrentHashMap<>();

  /**
   * Retrieves a {@link ViewWrapper} associated with the specified view identifier.
   * This method fetches the {@link ViewWrapper} from an internal cache if available.
   *
   * @param viewId the unique identifier of the view to retrieve
   * @return the {@link ViewWrapper} associated with the given view identifier,
   * or {@code null} if no such view exists in the cache
   */
  public ViewWrapper getView(UUID viewId) {
    return views.get(viewId);
  }

  /**
   * Adds a {@link ViewWrapper} to the internal cache. The {@link ViewWrapper} is stored using
   * its unique identifier as the key. If a {@link ViewWrapper} with the same identifier is
   * already present, it will be replaced.
   *
   * @param viewWrapper the {@link ViewWrapper} to add to the internal cache. It must contain a valid
   *                    unique identifier retrievable via {@link ViewWrapper#viewId()}.
   */
  public void addView(ViewWrapper viewWrapper) {
    views.put(viewWrapper.viewId(), viewWrapper);
  }

  /**
   * Removes a view associated with the given unique identifier and closes its resources.
   * This method retrieves the {@link ViewWrapper} corresponding to the specified identifier
   * from the internal cache, removes it, and invokes the {@code close()} method on its
   * underlying {@link View} instance. If an exception occurs during the closing of the
   * {@link View}, it is wrapped in a {@link RuntimeException} and rethrown.
   *
   * @param viewId the unique identifier of the view to remove and close
   * @throws RuntimeException if an exception occurs while closing the view
   */
  public void removeAndCloseView(UUID viewId) {
    val viewWrapper = views.remove(viewId);
    if (viewWrapper == null) {
      return;
    }
    try {
      viewWrapper.view().close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Closes every open view that belongs to the given VSUM (e.g. before delete or reopen).
   */
  public void removeAndCloseViewsForVsum(UUID vsumId) {
    views.entrySet().stream()
        .filter(entry -> entry.getValue().vsumWrapper().info().getId().equals(vsumId))
        .map(Map.Entry::getKey)
        .toList()
        .forEach(this::removeAndCloseView);
  }
}
