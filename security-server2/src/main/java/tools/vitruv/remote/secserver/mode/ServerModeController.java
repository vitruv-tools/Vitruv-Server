package tools.vitruv.remote.secserver.mode;

import org.eclipse.jetty.server.Handler;

import tools.vitruv.framework.remote.server.VitruviusServer;

public interface ServerModeController extends VitruviusServer {
    Handler getHandler();
}
