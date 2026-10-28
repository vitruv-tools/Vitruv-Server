package tools.vitruv.framework.remote.server.rest.endpoints;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintErrorDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintErrorDto.DiagnosticDto;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;
import tools.vitruv.framework.vsum.constraints.ConstraintNotEditableException;
import tools.vitruv.framework.vsum.constraints.DuplicateConstraintException;
import tools.vitruv.framework.vsum.constraints.InvalidConstraintException;
import tools.vitruv.framework.vsum.constraints.UnknownConstraintException;
import tools.vitruv.framework.vsum.constraints.UnsupportedConstraintLanguageException;

/**
 * Shared handling of requests to the constraint endpoints: the constraint id in the path, the
 * revision of the constraints, JSON bodies and the mapping of errors to status codes.
 *
 * <p>The id of a constraint is the URL-encoded path segment below {@code /vsum/constraints/}. The
 * segments {@code validate} and {@code revision} are reserved for their own endpoints.
 */
final class ConstraintRequests {
  private static final int HTTP_UNPROCESSABLE_ENTITY = 422;
  private static final String PATH_PREFIX = EndpointPath.CONSTRAINTS + "/";

  private ConstraintRequests() {}

  /**
   * Gets the id of the constraint addressed by the request path.
   *
   * @param wrapper the request
   * @return the decoded id, empty if the request addresses the collection of constraints
   * @throws ServerHaltingException with 404 if the path addresses no constraint
   */
  static Optional<String> idOf(HttpWrapper wrapper) {
    String path = wrapper.getRequestPath();
    if (path.equals(EndpointPath.CONSTRAINTS) || path.equals(PATH_PREFIX)) {
      return Optional.empty();
    }
    String segment = path.startsWith(PATH_PREFIX) ? path.substring(PATH_PREFIX.length()) : "";
    if (segment.isEmpty() || segment.contains("/")) {
      throw new ServerHaltingException(HTTP_NOT_FOUND, "Path " + path + " not found");
    }
    // keep a literal "+", URLDecoder would turn it into a space
    return Optional.of(
        URLDecoder.decode(segment.replace("+", "%2B"), StandardCharsets.UTF_8));
  }

  /**
   * Gets the id of the constraint addressed by the request path, which must address a constraint.
   *
   * @param wrapper the request
   * @param method the HTTP method, for the error message
   * @return the decoded id
   * @throws ServerHaltingException if the path addresses the collection of constraints
   */
  static String requireIdOf(HttpWrapper wrapper, String method) {
    return idOf(wrapper)
        .orElseThrow(
            () ->
                new ServerHaltingException(
                    HTTP_BAD_METHOD,
                    method + " requires the id of a constraint in the path"));
  }

  /**
   * Computes the revision of the constraints of the virtual model. It is derived from the
   * constraints themselves, so it changes with every change of a constraint and stays the same
   * across restarts as long as the constraints do.
   *
   * @param model the virtual model
   * @return the revision, quoted as an entity tag
   */
  static String revisionOf(VirtualModel model) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (ConstraintDescriptor constraint : model.getConstraints()) {
        update(digest, constraint.id());
        update(digest, constraint.language());
        update(digest, constraint.source());
        update(digest, constraint.kind() == null ? null : constraint.kind().name());
        update(digest, constraint.severity() == null ? null : constraint.severity().name());
      }
      return "\"" + HexFormat.of().formatHex(digest.digest(), 0, 16) + "\"";
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private static void update(MessageDigest digest, String value) {
    // length prefixes keep the boundaries between values unambiguous
    byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    digest.update(
        Integer.toString(value == null ? -1 : bytes.length).getBytes(StandardCharsets.UTF_8));
    digest.update((byte) ':');
    digest.update(bytes);
  }

  /**
   * Checks the {@code If-Match} header of the request against the current revision. Must be
   * called while the virtual model is locked, so the revision cannot change before the change.
   *
   * @param wrapper the request
   * @param model the virtual model
   * @throws ServerHaltingException with 412 if the constraints changed since the given revision
   */
  static void checkRevision(HttpWrapper wrapper, VirtualModel model) {
    String expected = wrapper.getRequestHeader(Header.IF_MATCH);
    if (expected == null || expected.isBlank() || expected.trim().equals("*")) {
      return;
    }
    String current = revisionOf(model);
    boolean matches =
        List.of(expected.split(",")).stream()
            .map(String::trim)
            .anyMatch(tag -> tag.equals(current) || tag.equals("W/" + current));
    if (!matches) {
      throw new ServerHaltingException(
          HTTP_PRECON_FAILED, "The constraints changed, the current revision is " + current);
    }
  }

  /**
   * Reads the JSON body of the request.
   *
   * @param wrapper the request
   * @param mapper the JSON mapper
   * @param type the type of the body
   * @return the body
   * @throws ServerHaltingException with 400 if the body is missing or malformed
   */
  static <T> T readBody(HttpWrapper wrapper, JsonMapper mapper, Class<T> type) {
    T body;
    try {
      String json = wrapper.getRequestBodyAsString();
      body = json == null || json.isBlank() ? null : mapper.deserialize(json, type);
    } catch (JsonProcessingException e) {
      throw badRequest("Invalid request: " + e.getOriginalMessage(), mapper);
    } catch (IOException e) {
      throw badRequest("Could not read request body", mapper);
    }
    if (body == null) {
      throw badRequest("The request body is missing", mapper);
    }
    return body;
  }

  /**
   * Checks that a field of a request body is given.
   *
   * @param value the value of the field
   * @param field the name of the field
   * @param mapper the JSON mapper
   * @return the value
   * @throws ServerHaltingException with 400 if the value is missing or blank
   */
  static String requireField(String value, String field, JsonMapper mapper) {
    if (value == null || value.isBlank()) {
      throw badRequest("The field '" + field + "' is missing", mapper);
    }
    return value;
  }

  /**
   * Sends the given body as JSON together with the current revision.
   *
   * @param wrapper the response
   * @param model the virtual model
   * @param mapper the JSON mapper
   * @param body the body, {@code null} for none
   * @return the serialized body to return from the endpoint
   */
  static String respond(HttpWrapper wrapper, VirtualModel model, JsonMapper mapper, Object body) {
    wrapper.addResponseHeader(Header.ETAG, revisionOf(model));
    if (body == null) {
      return null;
    }
    wrapper.setContentType(ContentType.APPLICATION_JSON);
    try {
      return mapper.serialize(body);
    } catch (JsonProcessingException e) {
      throw new ServerHaltingException(HTTP_INTERNAL_ERROR, e.getMessage());
    }
  }

  /**
   * Maps an exception of the constraint API of the virtual model to an error response.
   *
   * @param exception the exception
   * @param mapper the JSON mapper
   * @return the exception halting the request
   */
  static ServerHaltingException toError(RuntimeException exception, JsonMapper mapper) {
    if (exception instanceof ServerHaltingException halting) {
      return halting;
    }
    if (exception instanceof InvalidConstraintException invalid) {
      List<DiagnosticDto> diagnostics =
          invalid.getDiagnostics().stream().map(DiagnosticDto::from).toList();
      return error(HTTP_BAD_REQUEST, "Invalid constraint", diagnostics, mapper);
    }
    if (exception instanceof UnknownConstraintException) {
      return error(HTTP_NOT_FOUND, exception.getMessage(), List.of(), mapper);
    }
    if (exception instanceof DuplicateConstraintException
        || exception instanceof ConstraintNotEditableException) {
      return error(HTTP_CONFLICT, exception.getMessage(), List.of(), mapper);
    }
    if (exception instanceof UnsupportedConstraintLanguageException) {
      return error(HTTP_UNPROCESSABLE_ENTITY, exception.getMessage(), List.of(), mapper);
    }
    return error(
        HTTP_INTERNAL_ERROR,
        "Changing the constraints failed: " + exception.getMessage(),
        List.of(),
        mapper);
  }

  /**
   * Creates the error response for a request without a constraint.
   *
   * @param id the requested id
   * @param mapper the JSON mapper
   * @return the exception halting the request with 404
   */
  static ServerHaltingException notFound(String id, JsonMapper mapper) {
    return toError(new UnknownConstraintException(id), mapper);
  }

  private static ServerHaltingException badRequest(String message, JsonMapper mapper) {
    return error(HTTP_BAD_REQUEST, message, List.of(), mapper);
  }

  private static ServerHaltingException error(
      int status, String message, List<DiagnosticDto> diagnostics, JsonMapper mapper) {
    String json;
    try {
      json = mapper.serialize(new ConstraintErrorDto(message, diagnostics));
    } catch (JsonProcessingException e) {
      json = null;
    }
    return new ServerHaltingException(status, message, json);
  }
}
