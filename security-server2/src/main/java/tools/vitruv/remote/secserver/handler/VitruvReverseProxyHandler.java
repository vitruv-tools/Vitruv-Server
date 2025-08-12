package tools.vitruv.remote.secserver.handler;

import java.util.List;
import java.util.function.Function;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import tools.vitruv.framework.remote.client.jetty.JettyHttpClientFactory;
import tools.vitruv.framework.remote.common.AvailableHttpVersions;
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
        return JettyHttpClientFactory.createClearTextHttpClient(this.httpVersions);
    }

    @Override
    public boolean handle(Request clientToProxyRequest, Response proxyToClientResponse, Callback proxyToClientCallback) {
        if (!ReverseProxyMappingService.instance().canHandlePath(clientToProxyRequest.getHttpURI().getPath())) {
            return false;
        }
        return super.handle(clientToProxyRequest, proxyToClientResponse, proxyToClientCallback);
    }
}
