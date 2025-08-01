package tools.vitruv.remote.secserver.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This singleton provides a service for mapping URI paths of one domain to URIs of (potentially) multiple
 * domains for proxying requests to other servers. This service supports two .
 */
public class ReverseProxyMappingService {
    private static ReverseProxyMappingService instance;
    private final Pattern pathSeparatorPattern = Pattern.compile("^(/[\\p{Graph}\\p{Blank}&&[^/]]*)(/.*)?$");

    private String oneDestination = null;
    private Map<String, String> pathToUri = new HashMap<>();

    private ReverseProxyMappingService() {}

    public static ReverseProxyMappingService instance() {
        if (instance == null) {
            instance = new ReverseProxyMappingService();
        }

        return instance;
    }

    /**
     * Sets the one and only destination of redirects. As a result, every path is directly mapped to the given URI.
     * 
     * @param uri the URI to which all paths are mapped.
     */
    public void setOneDestination(String uri) {
        this.oneDestination = uri;
    }

    /**
     * Adds a mapping for a context path to another domain. As a result, paths of the form
     * <code>/&lt;contextPath&gt;(/&lt;further paths&gt;)</code> are mapped to
     * <code>&lt;targetUri&gt;(/&lt;further paths&gt;)</code>.
     * 
     * @param contextPath the first part of the path which is used to map the remaining path to the other domain.
     * @param targetUri the URI to which the path is mapped.
     */
    public void addDestination(String contextPath, String targetUri) {
        this.pathToUri.put(contextPath, targetUri);
    }

    /**
     * Removes a mapping for a context path to support dynamic configurations.
     * 
     * @param contextPath the context path to remove.
     */
    public void removeDestination(String contextPath) {
        this.pathToUri.remove(contextPath);
    }

    /**
     * Checks if a given path can be handled (i.e., either the one and only destination is set or there is a registered mapping for the path).
     * 
     * @param path the path to check.
     * @return true if the redirection for the path can be handled. false otherwise.
     */
    public boolean canHandlePath(String path) {
        var pathMatcher = this.pathSeparatorPattern.matcher(path);
        return this.oneDestination != null || (pathMatcher.find() && this.pathToUri.containsKey(pathMatcher.group(1)));
    }

    /**
     * Constructs the redirect URI for a path.
     * 
     * @param path the path to map.
     * @return the mapped path.
     * @throws IllegalStateException if the path is invalid or no destination URI can be found.
     */
    public String getRedirectUriForPath(String path) {
        if (this.oneDestination != null) {
            return this.oneDestination + path;
        }

        var pathMatcher = this.pathSeparatorPattern.matcher(path);
        if (!pathMatcher.find()) {
            throw new IllegalStateException("Given path is not valid.");
        }

        var contextPath = pathMatcher.group(1);
        if (!this.pathToUri.containsKey(contextPath)) {
            throw new IllegalStateException("No target found for path.");
        }
        var mappedUri = this.pathToUri.get(contextPath);

        var redirectPart = pathMatcher.group(2);
        redirectPart = redirectPart == null ? "" : redirectPart;
        
        return mappedUri + redirectPart;
    }
}
