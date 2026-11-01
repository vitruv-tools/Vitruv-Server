package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;

/** Shared handling of requests to the model and metamodel endpoints. */
final class ModelRequests {

  private ModelRequests() {}

  /**
   * Gets the id addressed by the request path below the given base path, e.g. the id of a model in
   * {@code /vsum/models/{id}}.
   *
   * @param wrapper the request
   * @param basePath the path of the collection, e.g. {@code /vsum/models}
   * @return the decoded id, empty if the request addresses the collection
   * @throws ServerHaltingException with 404 if the path addresses nothing below the base path
   */
  static Optional<String> idBelow(HttpWrapper wrapper, String basePath) {
    String path = wrapper.getRequestPath();
    String prefix = basePath + "/";
    if (path.equals(basePath) || path.equals(prefix)) {
      return Optional.empty();
    }
    String segment = path.startsWith(prefix) ? path.substring(prefix.length()) : "";
    // slashes of ids are URL-encoded, so a literal slash means an unknown path
    if (segment.isEmpty() || segment.contains("/")) {
      throw new ServerHaltingException(HTTP_NOT_FOUND, "Path " + path + " not found");
    }
    // keep a literal "+", URLDecoder would turn it into a space
    return Optional.of(URLDecoder.decode(segment.replace("+", "%2B"), StandardCharsets.UTF_8));
  }

  /**
   * Checks whether the client asks for XMI instead of JSON in the {@code Accept} header.
   *
   * @param wrapper the request
   * @return {@code true} for {@code application/xmi+xml} or {@code application/xml}
   */
  static boolean acceptsXmi(HttpWrapper wrapper) {
    String accept = wrapper.getRequestHeader(Header.ACCEPT);
    return accept != null && (accept.contains("xmi") || accept.contains("application/xml"));
  }

  /**
   * Serializes the given XMI resource and sets the content type of the response.
   *
   * @param wrapper the response
   * @param resource the resource
   * @return the XMI document
   */
  static String respondXmi(HttpWrapper wrapper, Resource resource) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      resource.save(out, Map.of(XMLResource.OPTION_ENCODING, StandardCharsets.UTF_8.name()));
    } catch (IOException e) {
      throw new ServerHaltingException(
          HTTP_INTERNAL_ERROR, "Could not serialize " + resource.getURI() + ": " + e.getMessage());
    }
    wrapper.setContentType(ContentType.APPLICATION_XMI);
    return out.toString(StandardCharsets.UTF_8);
  }

  /**
   * Creates the error for an unknown id.
   *
   * @param kind what the id should identify, e.g. {@code Model}
   * @param id the id
   * @return the error with 404
   */
  static ServerHaltingException notFound(String kind, String id) {
    return new ServerHaltingException(HTTP_NOT_FOUND, kind + " " + id + " not found");
  }
}
