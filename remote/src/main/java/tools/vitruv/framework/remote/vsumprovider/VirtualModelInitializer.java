package tools.vitruv.framework.remote.vsumprovider;

import tools.vitruv.framework.vsum.VirtualModel;

@FunctionalInterface
public interface VirtualModelInitializer {
    VirtualModel init();
}
