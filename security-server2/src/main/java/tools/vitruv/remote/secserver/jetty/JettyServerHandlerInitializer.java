package tools.vitruv.remote.secserver.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.eclipse.jetty.server.handler.GracefulHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;
import tools.vitruv.remote.secserver.handler.ApiHandler;
import tools.vitruv.remote.secserver.handler.VitruvReverseProxyHandler;

/**
 * This class initializes the handlers for a Jetty server.
 */
public class JettyServerHandlerInitializer {
    private JettyServerHandlerInitializer() {}

    public static void initializeHandlers(Server server, ServerHandlerConfiguration config) {
        GracefulHandler handler1Graceful = new GracefulHandler();

        CrossOriginHandler handler2Cors = new CrossOriginHandler();
        handler2Cors.setDeliverPreflightRequests(true);
        handler2Cors.setAllowedOriginPatterns(config.allowedOriginPatterns());
        
        GzipHandler handler3Compression = new GzipHandler();
        
        StatisticsHandler handler4Statistics = new StatisticsHandler();
        
        ContextHandler handler6Api = new ContextHandler(new ApiHandler(), "/api");
        handler6Api.setAllowNullPathInContext(true);
        
        var handler7Proxy = new VitruvReverseProxyHandler(config.httpVersions());
        
        Handler.Sequence handler5Sequence = new Handler.Sequence(handler6Api, handler7Proxy);

        handler1Graceful.setHandler(handler2Cors);
        handler2Cors.setHandler(handler3Compression);
        handler3Compression.setHandler(handler4Statistics);
        handler4Statistics.setHandler(handler5Sequence);
        
        server.setHandler(handler1Graceful);
        server.setStopTimeout(5000);
    }
}
