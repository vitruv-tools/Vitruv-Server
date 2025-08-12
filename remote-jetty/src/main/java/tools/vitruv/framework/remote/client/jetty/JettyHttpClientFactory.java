package tools.vitruv.framework.remote.client.jetty;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnectionFactory.Info;
import org.eclipse.jetty.io.ClientConnector;

import tools.vitruv.framework.remote.common.AvailableHttpVersions;

/**
 * A factory for Eclipse Jetty HTTP clients.
 */
public final class JettyHttpClientFactory {
    private JettyHttpClientFactory() {}

    /**
     * Creates an HTTP client, which supports clear-text HTTP/1.1 and clear-text HTTP/2.
     * 
     * @return the created {@link HttpClient}.
     */
    public static HttpClient createClearTextHttpClient() {
        return createClearTextHttpClient(List.of(AvailableHttpVersions.HTTP_1_1, AvailableHttpVersions.HTTP_2));
    }

    /**
     * Creates an clear-text only HTTP client. The conrete supported HTTP versions can be specified.
     * 
     * @param httpVersions The list of HTTP versions, which should be supported by the created client.
     * @return The created {@link HttpClient}.
     */
    public static HttpClient createClearTextHttpClient(List<AvailableHttpVersions> httpVersions) {
        List<Info> versionInfos = new ArrayList<>();
        ClientConnector clientConnector = new ClientConnector();

        if (httpVersions.contains(AvailableHttpVersions.HTTP_1_1)) {
            ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;
            versionInfos.add(http1);
        }
        if (httpVersions.contains(AvailableHttpVersions.HTTP_2)) {
            HTTP2Client http2Client = new HTTP2Client(clientConnector);
            ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
            versionInfos.add(0, http2);
        }
        
        return new HttpClient(new HttpClientTransportDynamic(clientConnector, versionInfos.toArray(new Info[versionInfos.size()])));
    }
}
