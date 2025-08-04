package tools.vitruv.framework.remote.server.http.jetty;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import tools.vitruv.framework.remote.server.http.HttpWrapper;

class JettyHttpWrapper implements HttpWrapper {
    private Request request;
    private Response response;
    private Callback callback;

    JettyHttpWrapper(Request request, Response response, Callback callback) {
        this.request = request;
        this.response = response;
        this.callback = callback;
    }

    @Override
    public String getRequestHeader(String header) {
        return this.request.getHeaders().get(header);
    }

    @Override
    public String getRequestBodyAsString() throws IOException {
        return Content.Source.asString(this.request);
    }

    @Override
    public void addResponseHeader(String header, String value) {
        this.response.getHeaders().add(header, value);
    }

    @Override
    public void setContentType(String type) {
        this.response.getHeaders().add("Content-Type", type);
    }

    @Override
    public void sendResponse(int responseCode) throws IOException {
        this.response.setStatus(responseCode);
        Content.Sink.write(this.response, true, "", callback);
    }

    @Override
    public void sendResponse(int responseCode, byte[] body) throws IOException {
        this.response.setStatus(responseCode);
        this.response.write(true, ByteBuffer.wrap(body), this.callback);
    }
}
