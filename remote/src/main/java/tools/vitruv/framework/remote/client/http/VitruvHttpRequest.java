package tools.vitruv.framework.remote.client.http;

import java.util.HashMap;
import java.util.Map;

public class VitruvHttpRequest {
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String PATCH = "PATCH";

    private String uri;
    private String method;
    private Map<String, String> header = new HashMap<>();
    private String body;

    private VitruvHttpRequest(String method, String uri) {
        this.method = method;
        this.uri = uri;
    }

    public static VitruvHttpRequest GET(String uri) {
        return new VitruvHttpRequest(GET, uri);
    }

    public static VitruvHttpRequest POST(String uri) {
        return new VitruvHttpRequest(POST, uri);
    }

    public static VitruvHttpRequest PUT(String uri) {
        return new VitruvHttpRequest(PUT, uri);
    }

    public static VitruvHttpRequest DELETE(String uri) {
        return new VitruvHttpRequest(DELETE, uri);
    }

    public static VitruvHttpRequest PATCH(String uri) {
        return new VitruvHttpRequest(PATCH, uri);
    }

    public static VitruvHttpRequest create(String method, String uri) {
        return new VitruvHttpRequest(method, uri);
    }

    public String getMethod() {
        return this.method;
    }

    public String getUri() {
        return this.uri;
    }

    public VitruvHttpRequest addHeader(String key, String value) {
        this.header.put(key, value);
        return this;
    }

    public Map<String, String> getHeaders() {
        return this.header;
    }

    public VitruvHttpRequest setBody(String body) {
        this.body = body;
        return this;
    }

    public String getBody() {
        return this.body;
    }

    public VitruvHttpResponseWrapper sendRequest(VitruvHttpClientWrapper client) throws Exception {
        return client.sendRequest(this);
    }
}
