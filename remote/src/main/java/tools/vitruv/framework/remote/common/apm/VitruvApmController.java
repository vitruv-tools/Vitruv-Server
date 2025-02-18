package tools.vitruv.framework.remote.common.apm;

import java.nio.file.Path;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;

/**
 * This class allows controlling the Vitruvius monitoring.
 */
public class VitruvApmController {
    
	private static VitruvStepMeterRegistry activeRegistry;

    /**
     * Constructor to initialize the monitoring.
     * 
     * @param output Path to a file in which all measurements are stored.
     */
    private  VitruvApmController(Path output) {
         if (ACTIVE_REGISTRY == null) {
            ACTIVE_REGISTRY = new VitruvStepMeterRegistry(new VitruvStepRegistryConfig(), Clock.SYSTEM, output);
        }
    }
    
    /**
     * Enables the monitoring for Vitruvius.
     * 
     * @param output Path to a file in which all measurements are stored.
     */
    public static void enable(Path output) {
        if (activeRegistry == null) {
            new VitruvApmController(output);  // This calls the private constructor
            Metrics.globalRegistry.add(activeRegistry);
        }
    }
    
    /**
     * Disables the monitoring for Vitruvius.
     */
    public static void disable() {
        if (activeRegistry != null) {
            activeRegistry.stop();
            Metrics.globalRegistry.remove(activeRegistry);
            activeRegistry = null;
        }
    }
}
