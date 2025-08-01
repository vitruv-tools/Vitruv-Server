package tools.vitruv.remote.secserver.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.io.ClientConnectionFactory.Info;
import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import tools.vitruv.remote.secserver.config.AvailableHttpVersions;
import tools.vitruv.remote.secserver.proxy.ReverseProxyMappingService;

public class VitruvReverseProxyHandler extends ProxyHandler.Reverse {
    private static class HttpURIRewriter implements Function<Request, HttpURI> {
        @Override
        public HttpURI apply(Request request) {
            var redirectUri = ReverseProxyMappingService.instance().getRedirectUriForPath(request.getHttpURI().getPath());
            return HttpURI.from(redirectUri);
        }
    }

    private List<AvailableHttpVersions> httpVersions;

    public VitruvReverseProxyHandler(List<AvailableHttpVersions> httpVersions) {
        super(new HttpURIRewriter());
        this.httpVersions = httpVersions;
    }

    @Override
    protected HttpClient newHttpClient() {
        List<Info> versionInfos = new ArrayList<>();
        ClientConnector clientConnector = new ClientConnector();

        if (this.httpVersions.contains(AvailableHttpVersions.HTTP_1_1)) {
            ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;
            versionInfos.add(http1);
        }
        if (this.httpVersions.contains(AvailableHttpVersions.HTTP_2)) {
            HTTP2Client http2Client = new HTTP2Client(clientConnector);
            ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
            versionInfos.add(0, http2);
        }
        
        return new HttpClient(new HttpClientTransportDynamic(clientConnector, versionInfos.toArray(new Info[versionInfos.size()])));
    }

    @Override
    public boolean handle(Request clientToProxyRequest, Response proxyToClientResponse, Callback proxyToClientCallback) {
        if (!ReverseProxyMappingService.instance().canHandlePath(clientToProxyRequest.getHttpURI().getPath())) {
            return false;
        }
        return super.handle(clientToProxyRequest, proxyToClientResponse, proxyToClientCallback);
    }
}
