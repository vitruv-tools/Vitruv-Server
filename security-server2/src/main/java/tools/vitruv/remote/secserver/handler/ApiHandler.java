package tools.vitruv.remote.secserver.handler;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class ApiHandler extends Handler.Abstract.NonBlocking {
    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        response.setStatus(404);
        Content.Sink.write(response, true, "<!DOCTYPE html><html><head></head><body><p>Not implemented!</p></body></html>", callback);
        return true;
    }
}
