package tools.vitruv.remote.secserver.jetty;

import org.eclipse.jetty.http.HttpFields.Mutable;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.Request;

/**
 * This {@link Customizer} for Jetty adds the <code>Alt-Svc</code> (alternative service) response header
 * to indicate clients that an HTTP/3 server is available.
 */
class Http3AlternativeServiceCustomizer implements Customizer {
    private int http3Port = -1;

    Http3AlternativeServiceCustomizer(int http3Port) {
        this.http3Port = http3Port;
    }

    @Override
    public Request customize(Request request, Mutable responseHeaders) {
        responseHeaders.add("Alt-Svc", "h3=\":" + this.http3Port + "\"");
        return request;
    }
}
