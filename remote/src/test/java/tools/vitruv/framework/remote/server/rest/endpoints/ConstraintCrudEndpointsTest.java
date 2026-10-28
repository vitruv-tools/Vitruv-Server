package tools.vitruv.framework.remote.server.rest.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.interaction.UserInteractionFactory;
import tools.vitruv.framework.remote.client.exception.BadServerResponseException;
import tools.vitruv.framework.remote.client.impl.VitruvRemoteConnection;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationRequest;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.constraints.ConstraintCheckContext;
import tools.vitruv.framework.vsum.constraints.ConstraintChecker;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;
import tools.vitruv.framework.vsum.constraints.ConstraintDiagnostic;
import tools.vitruv.framework.vsum.constraints.ConstraintKind;
import tools.vitruv.framework.vsum.constraints.ConstraintResult;
import tools.vitruv.framework.vsum.constraints.ConstraintSeverity;
import tools.vitruv.framework.vsum.constraints.ConstraintViolation;
import tools.vitruv.framework.vsum.constraints.DuplicateConstraintException;
import tools.vitruv.framework.vsum.constraints.EditableConstraintChecker;
import tools.vitruv.framework.vsum.constraints.InvalidConstraintException;
import tools.vitruv.framework.vsum.constraints.UnknownConstraintException;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/** Round trip through a real {@link VitruvServer} for creating, updating and deleting constraints. */
class ConstraintCrudEndpointsTest {
  private static final String EXTENSION = "ecore";
  private static final String LANGUAGE = "prefix";
  private static final String BUILT_IN = "noUppercaseB";
  private static final String READ_ONLY = "readOnly";

  private static boolean registeredFactory;

  @TempDir Path serverDir;
  @TempDir Path clientDir;

  private VitruvServer server;
  private InternalVirtualModel model;
  private VitruvRemoteConnection client;
  private HttpClient http;
  private String baseUrl;
  private final ObjectMapper json = new ObjectMapper();

  @BeforeAll
  static void registerResourceFactory() {
    registeredFactory =
        Resource.Factory.Registry.INSTANCE
                .getExtensionToFactoryMap()
                .putIfAbsent(EXTENSION, new XMIResourceFactoryImpl())
            == null;
  }

  @AfterAll
  static void unregisterResourceFactory() {
    if (registeredFactory) {
      Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove(EXTENSION);
    }
  }

  @BeforeEach
  void startServer() throws IOException {
    startServer(true);
  }

  private void startServer(boolean seed) throws IOException {
    int port = freePort();
    server =
        new VitruvServer(
            () -> {
              try {
                model =
                    new VirtualModelBuilder()
                        .withStorageFolder(serverDir)
                        .withUserInteractorForResultProvider(
                            UserInteractionFactory.instance
                                .createPredefinedInteractionResultProvider(null))
                        .withConstraintCheckers(
                            new PrefixChecker(BUILT_IN + "=B"), new ReadOnlyChecker())
                        .buildAndInitialize();
                if (seed) {
                  seed(model, "good", "Bad");
                }
                return model;
              } catch (IOException e) {
                throw new IllegalStateException(e);
              }
            },
            port,
            "localhost");
    server.start();
    baseUrl = "http://localhost:" + port;
    client = new VitruvRemoteConnection("http", "localhost", port, clientDir);
    http = HttpClient.newHttpClient();
  }

  @AfterEach
  void stopServer() {
    server.stop();
    model.dispose();
  }

  @Test
  void createdConstraintIsListedAndValidated() throws Exception {
    HttpResponse<String> created =
        send("POST", "/vsum/constraints", "{\"language\": \"prefix\", \"source\": \"noG=g\"}");

    assertEquals(201, created.statusCode());
    assertEquals("/vsum/constraints/noG", created.headers().firstValue("Location").orElseThrow());
    assertTrue(created.headers().firstValue("ETag").isPresent());
    assertEquals("noG", json.readTree(created.body()).get("id").asText());

    HttpResponse<String> read = send("GET", "/vsum/constraints/noG", null);
    assertEquals(200, read.statusCode());
    assertEquals("noG=g", json.readTree(read.body()).get("source").asText());
    assertEquals(
        List.of(BUILT_IN, "noG", READ_ONLY),
        client.getConstraints().stream().map(ConstraintDto::id).toList());
    assertEquals("VIOLATED", status("noG"));
  }

  @Test
  void updatedConstraintIsValidatedWithItsNewSource() throws Exception {
    assertEquals("VIOLATED", status(BUILT_IN));

    HttpResponse<String> updated =
        send("PUT", "/vsum/constraints/" + BUILT_IN, "{\"source\": \"noUppercaseB=X\"}");

    assertEquals(200, updated.statusCode());
    assertEquals("noUppercaseB=X", json.readTree(updated.body()).get("source").asText());
    assertEquals("SATISFIED", status(BUILT_IN));
  }

  @Test
  void deletedConstraintIsGone() throws Exception {
    HttpResponse<String> deleted = send("DELETE", "/vsum/constraints/" + BUILT_IN, null);

    assertEquals(204, deleted.statusCode());
    assertTrue(deleted.headers().firstValue("ETag").isPresent());
    assertEquals(404, send("GET", "/vsum/constraints/" + BUILT_IN, null).statusCode());
    assertEquals(404, send("DELETE", "/vsum/constraints/" + BUILT_IN, null).statusCode());
    assertEquals(
        404,
        send(
                "POST",
                "/vsum/constraints/validate",
                "{\"constraintIds\": [\"" + BUILT_IN + "\"]}")
            .statusCode());
  }

  @Test
  void invalidSourceIsRejectedWithDiagnostics() throws Exception {
    HttpResponse<String> response =
        send("POST", "/vsum/constraints", "{\"language\": \"prefix\", \"source\": \"broken\"}");

    assertEquals(400, response.statusCode());
    assertEquals("application/json", response.headers().firstValue("Content-Type").orElseThrow());
    JsonNode body = json.readTree(response.body());
    assertEquals("Invalid constraint", body.get("error").asText());
    JsonNode diagnostic = body.get("diagnostics").get(0);
    assertEquals(1, diagnostic.get("line").asInt());
    assertEquals(0, diagnostic.get("column").asInt());
    assertEquals("Expected <id>=<prefix>", diagnostic.get("message").asText());
    assertEquals(2, client.getConstraints().size());

    assertEquals(
        400,
        send("PUT", "/vsum/constraints/" + BUILT_IN, "{\"source\": \"broken\"}").statusCode());
    assertEquals(BUILT_IN + "=B", client.getConstraint(BUILT_IN).orElseThrow().source());
  }

  @Test
  void conflictsAndUnsupportedLanguagesAreRejected() throws Exception {
    assertEquals(
        409,
        send("POST", "/vsum/constraints", "{\"language\": \"prefix\", \"source\": \"noUppercaseB=C\"}")
            .statusCode());
    assertEquals(
        422,
        send("POST", "/vsum/constraints", "{\"language\": \"ocl\", \"source\": \"a=b\"}")
            .statusCode());
    assertEquals(409, send("DELETE", "/vsum/constraints/" + READ_ONLY, null).statusCode());
    assertEquals(
        409,
        send("PUT", "/vsum/constraints/" + READ_ONLY, "{\"source\": \"readOnly=x\"}")
            .statusCode());
    assertEquals(
        404,
        send("PUT", "/vsum/constraints/unknown", "{\"source\": \"unknown=x\"}").statusCode());
  }

  @Test
  void malformedRequestsAreRejected() throws Exception {
    assertEquals(400, send("POST", "/vsum/constraints", "{not json").statusCode());
    assertEquals(400, send("POST", "/vsum/constraints", "").statusCode());
    assertEquals(400, send("POST", "/vsum/constraints", "{\"source\": \"a=b\"}").statusCode());
    assertEquals(400, send("PUT", "/vsum/constraints/" + BUILT_IN, "{}").statusCode());
    assertEquals(405, send("PUT", "/vsum/constraints", "{\"source\": \"a=b\"}").statusCode());
    assertEquals(405, send("DELETE", "/vsum/constraints", null).statusCode());
    assertEquals(
        405,
        send("POST", "/vsum/constraints/" + BUILT_IN, "{\"language\": \"prefix\", \"source\": \"a=b\"}")
            .statusCode());
    assertEquals(404, send("GET", "/vsum/constraints/a/b", null).statusCode());
    assertEquals(404, send("GET", "/vsum/constraintsX", null).statusCode());
  }

  @Test
  void revisionChangesWithTheConstraints() throws Exception {
    HttpResponse<String> list = send("GET", "/vsum/constraints", null);
    String revision = list.headers().firstValue("ETag").orElseThrow();
    assertEquals(revision, client.getConstraintRevision());

    client.createConstraint(LANGUAGE, "noG=g");

    String changed = client.getConstraintRevision();
    assertNotEquals(revision, changed);
    assertEquals(
        changed, send("GET", "/vsum/constraints/noG", null).headers().firstValue("ETag").get());
  }

  @Test
  void changesAreGuardedByTheRevision() throws Exception {
    String revision = client.getConstraintRevision();
    client.createConstraint(LANGUAGE, "noG=g");

    HttpResponse<String> stale =
        send("DELETE", "/vsum/constraints/noG", null, "If-Match", revision);

    assertEquals(412, stale.statusCode());
    assertTrue(client.getConstraint("noG").isPresent());
    assertEquals(
        204,
        send("DELETE", "/vsum/constraints/noG", null, "If-Match", client.getConstraintRevision())
            .statusCode());
  }

  @Test
  void idsAreUrlEncodedInPaths() throws Exception {
    ConstraintDto created = client.createConstraint(LANGUAGE, "shop::Item cheap+x=Z");

    assertEquals("shop::Item cheap+x", created.id());
    assertEquals(created, client.getConstraint(created.id()).orElseThrow());
    assertEquals(
        200, send("GET", "/vsum/constraints/shop%3A%3AItem%20cheap%2Bx", null).statusCode());

    client.updateConstraint(created.id(), "shop::Item cheap+x=Y");
    client.deleteConstraint(created.id());

    assertTrue(client.getConstraint(created.id()).isEmpty());
  }

  @Test
  void clientReportsErrorsWithTheirStatus() {
    BadServerResponseException invalid =
        assertThrows(
            BadServerResponseException.class, () -> client.createConstraint(LANGUAGE, "broken"));
    assertEquals(400, invalid.getStatusCode());
    assertTrue(invalid.getMessage().contains("Expected <id>=<prefix>"));
    assertEquals(
        404,
        assertThrows(BadServerResponseException.class, () -> client.deleteConstraint("unknown"))
            .getStatusCode());
  }

  @Test
  void changesSurviveARestart() throws Exception {
    client.createConstraint(LANGUAGE, "noG=g");
    client.deleteConstraint(BUILT_IN);
    stopServer();

    startServer(false);

    assertEquals(
        List.of("noG", READ_ONLY),
        client.getConstraints().stream().map(ConstraintDto::id).toList());
    assertEquals("VIOLATED", status("noG"));
  }

  private String status(String id) {
    return client
        .validateConstraints(new ConstraintValidationRequest(List.of(id), false))
        .results()
        .get(0)
        .status();
  }

  private HttpResponse<String> send(String method, String path, String body, String... headers)
      throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .method(
                method,
                body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
    if (headers.length > 0) {
      request.headers(headers);
    }
    return http.send(request.build(), BodyHandlers.ofString());
  }

  private void seed(VirtualModel virtualModel, String... packageNames) {
    var selector =
        virtualModel.createSelector(ViewTypeFactory.createIdentityMappingViewType("seeding"));
    try (CommittableView view = selector.createView().withChangeRecordingTrait()) {
      for (String name : packageNames) {
        EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setName(name);
        view.registerRoot(
            ePackage,
            org.eclipse.emf.common.util.URI.createFileURI(
                serverDir.resolve(name + "." + EXTENSION).toString()));
      }
      view.commitChanges();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  /**
   * An editable checker whose constraints have the source {@code <id>=<prefix>} and are violated by
   * every package whose name starts with the prefix.
   */
  private static final class PrefixChecker implements EditableConstraintChecker {
    private final Map<String, ConstraintDescriptor> constraints = new LinkedHashMap<>();

    PrefixChecker(String... builtInSources) {
      for (String source : builtInSources) {
        create(source);
      }
    }

    @Override
    public String getLanguage() {
      return LANGUAGE;
    }

    @Override
    public List<ConstraintDescriptor> getConstraints() {
      return List.copyOf(constraints.values());
    }

    @Override
    public ConstraintDescriptor create(String source) {
      ConstraintDescriptor descriptor = parse(source);
      if (constraints.containsKey(descriptor.id())) {
        throw new DuplicateConstraintException(descriptor.id());
      }
      constraints.put(descriptor.id(), descriptor);
      return descriptor;
    }

    @Override
    public ConstraintDescriptor update(String id, String source) {
      if (!constraints.containsKey(id)) {
        throw new UnknownConstraintException(id);
      }
      ConstraintDescriptor descriptor = parse(source);
      if (!descriptor.id().equals(id)) {
        throw new InvalidConstraintException(
            List.of(new ConstraintDiagnostic(1, 0, "The id cannot be changed")));
      }
      constraints.put(id, descriptor);
      return descriptor;
    }

    @Override
    public void delete(String id) {
      if (constraints.remove(id) == null) {
        throw new UnknownConstraintException(id);
      }
    }

    @Override
    public List<ConstraintResult> check(
        ConstraintCheckContext context, Collection<String> constraintIds) {
      return constraintIds.stream()
          .map(constraints::get)
          .map(descriptor -> ConstraintResult.of(descriptor, violations(context, descriptor)))
          .toList();
    }

    private static List<ConstraintViolation> violations(
        ConstraintCheckContext context, ConstraintDescriptor descriptor) {
      String prefix = descriptor.source().substring(descriptor.source().indexOf('=') + 1);
      return context.getModelResources().stream()
          .flatMap(resource -> resource.getContents().stream())
          .filter(root -> root instanceof EPackage ePackage && ePackage.getName().startsWith(prefix))
          .map(root -> new ConstraintViolation(root, ConstraintSeverity.MAJOR, prefix))
          .toList();
    }

    private static ConstraintDescriptor parse(String source) {
      int separator = source.indexOf('=');
      if (separator <= 0) {
        throw new InvalidConstraintException(
            List.of(new ConstraintDiagnostic(1, 0, "Expected <id>=<prefix>")));
      }
      String id = source.substring(0, separator);
      return new ConstraintDescriptor(
          id, id, "ecore::EPackage", ConstraintKind.INVARIANT, null, LANGUAGE, source);
    }
  }

  /** A checker whose constraint cannot be changed at runtime. */
  private static final class ReadOnlyChecker implements ConstraintChecker {
    private static final ConstraintDescriptor DESCRIPTOR =
        new ConstraintDescriptor(READ_ONLY, null, null, null, null, "fixed", null);

    @Override
    public List<ConstraintDescriptor> getConstraints() {
      return List.of(DESCRIPTOR);
    }

    @Override
    public List<ConstraintResult> check(
        ConstraintCheckContext context, Collection<String> constraintIds) {
      return List.of(ConstraintResult.satisfied(DESCRIPTOR));
    }
  }
}
