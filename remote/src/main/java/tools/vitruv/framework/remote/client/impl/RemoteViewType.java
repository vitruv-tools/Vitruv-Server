package tools.vitruv.framework.remote.client.impl;

import tools.vitruv.framework.views.ChangeableViewSource;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewType;

/**
 * A Vitruvius view type representing actual types on the virtual model, but is
 * still capable of providing a view selector and allows creating
 * views by querying the Vitruvius server.
 */
public class RemoteViewType implements ViewType<ViewSelector> {

    private final String name;
    private final SelectorProvider selectorProvider;

    public RemoteViewType(String name, SelectorProvider selectorProvider) {
        this.name = name;
        this.selectorProvider = selectorProvider;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ViewSelector createSelector(ChangeableViewSource viewSource) {
        return selectorProvider.getSelector(name);
    }
}

/**
 * Returns the {@link ViewSelector} of the {@link ViewType}, which allows
 * configuring views by delegating the request to the Vitruvius server.
 * 
 * @param viewSource Ignored, can be null.
 * @return A view selector for the view type represented by this remote view
 *         type.
 */
