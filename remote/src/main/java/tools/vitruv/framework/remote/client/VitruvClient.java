package tools.vitruv.framework.remote.client;

import tools.vitruv.framework.views.ViewProvider;
import tools.vitruv.framework.views.ViewTypeProvider;

/**
 * A Vitruvius client can remotely access the available {@link tools.vitruv.framework.views.ViewType}s of a Vitruvius instance and query
 * {@link tools.vitruv.framework.views.ViewSelector}s in order to create remotely editable {@link tools.vitruv.framework.views.View}s.
 */
public interface VitruvClient extends ViewTypeProvider, ViewProvider {
    /**
     * Disconnects this Vitruvius client from the remote Vitruvius server.
     * It also cleans up underlying resources, such as HTTP clients.
     * 
     * @throws Exception if the client cannot disconnect or the disconnect process fails.
     */
    public void disconnect() throws Exception;
}
