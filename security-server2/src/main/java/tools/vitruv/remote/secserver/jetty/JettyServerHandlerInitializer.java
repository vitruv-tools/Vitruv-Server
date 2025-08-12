package tools.vitruv.remote.secserver.jetty;

import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.openid.OpenIdAuthenticator;
import org.eclipse.jetty.security.openid.OpenIdLoginService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.eclipse.jetty.server.handler.GracefulHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.session.SessionHandler;

import tools.vitruv.remote.secserver.SessionConstants;
import tools.vitruv.remote.secserver.config.ServerHandlerConfiguration;
import tools.vitruv.remote.secserver.handler.ApiHandler;
import tools.vitruv.remote.secserver.handler.ApiPaths;

/**
 * This class initializes the handlers for a Jetty server.
 */
public class JettyServerHandlerInitializer {
    private JettyServerHandlerInitializer() {}

    public static void initializeHandlers(Server server, ServerHandlerConfiguration config, Handler actualHandler) {
        GracefulHandler handler1Graceful = new GracefulHandler();

        CrossOriginHandler handler2Cors = new CrossOriginHandler();
        handler2Cors.setDeliverPreflightRequests(true);
        handler2Cors.setAllowedOriginPatterns(config.allowedOriginPatterns());
        
        GzipHandler handler3Compression = new GzipHandler();
        
        StatisticsHandler handler4Statistics = new StatisticsHandler();

        var handler6Session = new SessionHandler();
        handler6Session.setHttpOnly(true);
        handler6Session.setSecureCookies(true);
        handler6Session.setSessionCookie(SessionConstants.SESSION_COOKIE_NAME);

        var handler5RootContext = new ContextHandler(handler6Session, "/");

        SecurityHandler.PathMapped handler7Security = new SecurityHandler.PathMapped();
        handler7Security.put("", Constraint.SECURE_TRANSPORT);
        handler7Security.put("/*", Constraint.ANY_USER);
        handler7Security.put("/favicon.ico", Constraint.ALLOWED);
        handler7Security.put(ApiPaths.OPENID_REDIRECT_PATH, Constraint.ALLOWED);
        handler7Security.put(ApiPaths.OPENID_ERROR_PATH, Constraint.ALLOWED);
        handler7Security.put(ApiPaths.OPENID_FULL_LOGOUT_REDIRECT_PATH, Constraint.ALLOWED);

        LoginService loginService = new OpenIdLoginService(config.openIdConfig());
        handler7Security.setLoginService(loginService);
        OpenIdAuthenticator oidAuth = new OpenIdAuthenticator(
                config.openIdConfig(),
                ApiPaths.OPENID_REDIRECT_PATH,
                ApiPaths.OPENID_ERROR_PATH,
                ApiPaths.OPENID_LOGOUT_REDIRECT_PATH
            );
        handler7Security.setAuthenticator(oidAuth);
        
        ContextHandler handler9Api = new ContextHandler(new ApiHandler(oidAuth), "/api");
        handler9Api.setAllowNullPathInContext(true);
        
        Handler.Sequence handler8Sequence = new Handler.Sequence(handler9Api, actualHandler);

        handler1Graceful.setHandler(handler2Cors);
        handler2Cors.setHandler(handler3Compression);
        handler3Compression.setHandler(handler4Statistics);
        handler4Statistics.setHandler(handler5RootContext);
        handler6Session.setHandler(handler7Security);
        handler7Security.setHandler(handler8Sequence);
        
        server.setHandler(handler1Graceful);
        server.setStopTimeout(5000);
    }
}
