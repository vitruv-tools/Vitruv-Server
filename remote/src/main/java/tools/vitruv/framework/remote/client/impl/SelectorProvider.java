package tools.vitruv.framework.remote.client.impl;

import tools.vitruv.framework.views.ViewSelector;

public interface SelectorProvider {
    ViewSelector getSelector(String viewTypeName);
}

