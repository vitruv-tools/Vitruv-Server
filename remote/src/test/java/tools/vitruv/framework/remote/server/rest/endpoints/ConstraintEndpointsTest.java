package tools.vitruv.framework.remote.server.rest.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emfcloud.jackson.resource.JsonResource;
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
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintResultDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintStatusDto;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationRequest;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintValidationResponse;
import tools.vitruv.framework.remote.common.rest.constraints.ViolationDto;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.constraints.ConstraintCheckContext;
import tools.vitruv.framework.vsum.constraints.ConstraintChecker;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;
import tools.vitruv.framework.vsum.constraints.ConstraintKind;
import tools.vitruv.framework.vsum.constraints.ConstraintResult;
import tools.vitruv.framework.vsum.constraints.ConstraintSeverity;
import tools.vitruv.framework.vsum.constraints.ConstraintViolation;

/** Round trip through a real {@link VitruvServer} for listing and validating constraints. */
class ConstraintEndpointsTest {
  private static final String EXTENSION = "ecore";
  private static final String LOWERCASE = "lowercasePackageNames";
  private static final String HAS_PACKAGES = "hasPackages";

  private static boolean registeredFactory;

  @TempDir Path serverDir;
  @TempDir Path clientDir;

  private VitruvServer server;
  private VitruvRemoteConnection client;
  private HttpClient http;
  private String baseUrl;

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
    int port = freePort();
    server =
        new VitruvServer(
            () -> {
              try {
                VirtualModel model =
                    new VirtualModelBuilder()
                        .withStorageFolder(serverDir)
                        .withUserInteractorForResultProvider(
                            UserInteractionFactory.instance
                                .createPredefinedInteractionResultProvider(null))
                        .withConstraintCheckers(new LowercaseChecker(), new HasPackagesChecker())
                        .buildAndInitialize();
                seed(model, "good", "Bad");
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
  }

  @Test
  void listsAllConstraints() {
    List<ConstraintDto> constraints = client.getConstraints();

    assertEquals(
        List.of(LOWERCASE, HAS_PACKAGES), constraints.stream().map(ConstraintDto::id).toList());
    ConstraintDto first = constraints.get(0);
    assertEquals("ecore::EPackage", first.contextType());
    assertEquals("INVARIANT", first.kind());
    assertEquals("CRITICAL", first.severity());
    assertEquals("test", first.language());
    assertEquals(List.of("ecore", "other"), first.metamodels());
    assertEquals(List.of(), constraints.get(1).metamodels());
  }

  @Test
  void listsTheMetamodelsOfConstraintsAsJson() throws Exception {
    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/vsum/constraints")).GET().build(),
                BodyHandlers.ofString());

    JsonNode first = new ObjectMapper().readTree(response.body()).get(0);
    assertEquals("ecore", first.get("metamodels").get(0).asText());
    assertEquals("other", first.get("metamodels").get(1).asText());
  }

  @Test
  void readsAnswersWithoutMetamodels() {
    ConstraintDto dto = new ConstraintDto("c", "c", null, "INVARIANT", "WARNING", null, null, null);

    assertEquals(List.of(), dto.metamodels());
  }

  @Test
  void validatesAllConstraintsWithViolationUuids() {
    ConstraintValidationResponse response =
        client.validateConstraints(new ConstraintValidationRequest(null, false));

    assertFalse(response.satisfied());
    assertNull(response.models());
    ConstraintResultDto lowercase = resultFor(response, LOWERCASE);
    assertEquals("VIOLATED", lowercase.status());
    assertEquals(1, lowercase.violations().size());
    ViolationDto violation = lowercase.violations().get(0);
    assertNotNull(violation.uuid());
    assertFalse(violation.uuid().startsWith("Uuid("));
    assertEquals("ecore::EPackage", violation.eClass());
    assertEquals("/seeded.ecore", violation.resource());
    assertEquals("Bad must be lowercase", violation.message());
    assertEquals("SATISFIED", resultFor(response, HAS_PACKAGES).status());
  }

  @Test
  void returnsTheStatusOfAllConstraints() throws Exception {
    HttpResponse<String> response = get("/vsum/constraints/status");

    assertEquals(200, response.statusCode());
    JsonNode json = new ObjectMapper().readTree(response.body());
    assertFalse(json.get("satisfied").asBoolean());
    assertEquals("VIOLATED", json.get("constraints").get(LOWERCASE).asText());
    assertEquals("SATISFIED", json.get("constraints").get(HAS_PACKAGES).asText());
    List<String> order = new ArrayList<>();
    json.get("constraints").fieldNames().forEachRemaining(order::add);
    assertEquals(List.of(LOWERCASE, HAS_PACKAGES), order);
    assertEquals(response.headers().firstValue("ETag").orElseThrow(), json.get("revision").asText());
    assertFalse(json.has("results"), "no violations are sent");
  }

  @Test
  void returnsTheStatusOfSelectedConstraints() throws Exception {
    HttpResponse<String> response =
        get("/vsum/constraints/status?id=" + LOWERCASE.replace(":", "%3A"));

    JsonNode constraints = new ObjectMapper().readTree(response.body()).get("constraints");
    assertEquals(1, constraints.size());
    assertEquals("VIOLATED", constraints.get(LOWERCASE).asText());
  }

  @Test
  void rejectsUnknownAndEmptyIdsInTheStatus() throws Exception {
    assertEquals(404, get("/vsum/constraints/status?id=unknown").statusCode());
    assertEquals(400, get("/vsum/constraints/status?id=").statusCode());
  }

  @Test
  void returnsTheStatusViaClient() {
    ConstraintStatusDto all = client.getConstraintStatus(null);
    ConstraintStatusDto selected = client.getConstraintStatus(List.of(HAS_PACKAGES));
    ConstraintStatusDto none = client.getConstraintStatus(List.of());

    assertFalse(all.satisfied());
    assertEquals("VIOLATED", all.constraints().get(LOWERCASE));
    assertEquals(client.getConstraintRevision(), all.revision());
    assertTrue(selected.satisfied());
    assertEquals(List.of(HAS_PACKAGES), List.copyOf(selected.constraints().keySet()));
    assertTrue(none.constraints().isEmpty());
    assertTrue(none.satisfied());
    BadServerResponseException unknown =
        assertThrows(
            BadServerResponseException.class, () -> client.getConstraintStatus(List.of("x")));
    assertEquals(404, unknown.getStatusCode());
  }

  @Test
  void statusEqualsTheValidation() {
    ConstraintValidationResponse validation =
        client.validateConstraints(new ConstraintValidationRequest(null, false));
    ConstraintStatusDto status = client.getConstraintStatus(null);

    assertEquals(validation.satisfied(), status.satisfied());
    for (ConstraintResultDto result : validation.results()) {
      assertEquals(result.status(), status.constraints().get(result.constraintId()));
    }
  }

  @Test
  void validatesSelectedConstraintsOnly() {
    ConstraintValidationResponse response =
        client.validateConstraints(new ConstraintValidationRequest(List.of(HAS_PACKAGES), false));

    assertEquals(1, response.results().size());
    assertTrue(response.satisfied());
  }

  @Test
  void returnsAffectedModelsAnnotatedWithUuids() throws Exception {
    HttpResponse<String> response =
        post("/vsum/constraints/validate", "{\"includeModels\": true}");

    assertEquals(200, response.statusCode());
    JsonNode json = new ObjectMapper().readTree(response.body());
    JsonNode violation = json.get("results").get(0).get("violations").get(0);
    String uuid = violation.get("uuid").asText();
    JsonNode models = json.get("models");
    assertEquals(1, models.size());
    assertEquals(violation.get("resource").asText(), models.get(0).get("uri").asText());
    List<String> ids = new ArrayList<>();
    models.get(0).get("content").findValues("_id").forEach(id -> ids.add(id.asText()));
    assertTrue(ids.contains(uuid), "the violating element is annotated with its UUID");
    assertTrue(ids.size() >= 4, "all elements of the model are annotated with their UUIDs");
  }

  @Test
  void referencesToOtherModelsUseRelativeUriAndUuid() throws Exception {
    HttpResponse<String> response =
        post("/vsum/constraints/validate", "{\"includeModels\": true}");

    JsonNode models = new ObjectMapper().readTree(response.body()).get("models");
    String content = models.toString();
    assertFalse(content.contains("file:"), "no absolute server paths: " + content);
    List<String> refs = new ArrayList<>();
    models.findValues("$ref").forEach(ref -> refs.add(ref.asText()));
    List<String> external = refs.stream().filter(ref -> ref.startsWith("other.ecore#")).toList();
    assertEquals(1, external.size(), "one reference into the unaffected model: " + refs);
    assertFalse(
        external.get(0).substring("other.ecore#".length()).startsWith("/"),
        "the target is identified by its UUID, not by its path: " + external);
  }

  @Test
  void returnsModelsViaClient() {
    ConstraintValidationResponse response =
        client.validateConstraints(new ConstraintValidationRequest(List.of(LOWERCASE), true));

    assertNotNull(response.models());
    String uuid = resultFor(response, LOWERCASE).violations().get(0).uuid();
    var resource =
        (JsonResource)
            response.models().getResources().stream()
                .filter(r -> r.getURI().lastSegment().equals("seeded." + EXTENSION))
                .findFirst()
                .orElseThrow();
    var violating = (EPackage) resource.getEObject(uuid);
    assertEquals("Bad", violating.getName());
  }

  @Test
  void validatesNothingForEmptySelection() throws Exception {
    HttpResponse<String> response =
        post("/vsum/constraints/validate", "{\"constraintIds\": []}");

    JsonNode json = new ObjectMapper().readTree(response.body());
    assertEquals(200, response.statusCode());
    assertEquals(0, json.get("results").size());
    assertTrue(json.get("satisfied").asBoolean());
  }

  @Test
  void treatsNullBodyLikeEmptyBody() throws Exception {
    HttpResponse<String> response = post("/vsum/constraints/validate", "null");

    assertEquals(200, response.statusCode());
    assertEquals(2, new ObjectMapper().readTree(response.body()).get("results").size());
  }

  @Test
  void rejectsEmptyConstraintIds() throws Exception {
    assertEquals(
        400, post("/vsum/constraints/validate", "{\"constraintIds\": [null]}").statusCode());
  }

  @Test
  void validatesAllConstraintsForEmptyBody() throws Exception {
    HttpResponse<String> response = post("/vsum/constraints/validate", "");

    assertEquals(200, response.statusCode());
    assertEquals(2, new ObjectMapper().readTree(response.body()).get("results").size());
  }

  @Test
  void rejectsUnknownConstraints() throws Exception {
    assertEquals(
        404,
        post("/vsum/constraints/validate", "{\"constraintIds\": [\"unknown\"]}").statusCode());
    assertThrows(
        BadServerResponseException.class,
        () ->
            client.validateConstraints(
                new ConstraintValidationRequest(List.of("unknown"), false)));
  }

  @Test
  void rejectsMalformedRequests() throws Exception {
    assertEquals(400, post("/vsum/constraints/validate", "{not json").statusCode());
  }

  @Test
  void returnsModelsWhenRequestedInTheQuery() throws Exception {
    JsonNode withFlag =
        new ObjectMapper()
            .readTree(post("/vsum/constraints/validate?includeModels", "").body());
    JsonNode withTrue =
        new ObjectMapper()
            .readTree(post("/vsum/constraints/validate?includeModels=true", "").body());
    JsonNode overridden =
        new ObjectMapper()
            .readTree(
                post("/vsum/constraints/validate?includeModels=false", "{\"includeModels\": true}")
                    .body());

    assertEquals(1, withFlag.get("models").size());
    assertEquals(1, withTrue.get("models").size());
    assertTrue(
        overridden.get("models") == null || overridden.get("models").isNull(),
        "the query overrides the body");
    assertEquals(
        400, post("/vsum/constraints/validate?includeModels=maybe", "").statusCode());
  }

  @Test
  void rejectsInvalidPercentEncodings() throws Exception {
    for (String path :
        List.of("/vsum/constraints/%zz", "/vsum/models/%zz", "/vsum/constraints/status?id=%zz")) {
      var connection =
          (java.net.HttpURLConnection) new java.net.URL(baseUrl + path).openConnection();
      try {
        assertEquals(400, connection.getResponseCode(), path);
      } finally {
        connection.disconnect();
      }
    }
  }

  private HttpResponse<String> get(String path) throws Exception {
    return http.send(
        HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(), BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return http.send(request, BodyHandlers.ofString());
  }

  private static ConstraintResultDto resultFor(ConstraintValidationResponse response, String id) {
    return response.results().stream()
        .filter(result -> result.constraintId().equals(id))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Seeds {@code seeded.ecore} with the given packages, the last one containing a class that
   * extends a class of the same model and a class of {@code other.ecore}.
   */
  private void seed(VirtualModel model, String... packageNames) {
    var selector = model.createSelector(ViewTypeFactory.createIdentityMappingViewType("seeding"));
    try (CommittableView view = selector.createView().withChangeRecordingTrait()) {
      EPackage other = EcoreFactory.eINSTANCE.createEPackage();
      other.setName("base");
      EClass base = EcoreFactory.eINSTANCE.createEClass();
      base.setName("Base");
      other.getEClassifiers().add(base);

      EPackage root = EcoreFactory.eINSTANCE.createEPackage();
      root.setName(packageNames[0]);
      EPackage last = root;
      for (int i = 1; i < packageNames.length; i++) {
        EPackage sub = EcoreFactory.eINSTANCE.createEPackage();
        sub.setName(packageNames[i]);
        root.getESubpackages().add(sub);
        last = sub;
      }
      EClass local = EcoreFactory.eINSTANCE.createEClass();
      local.setName("Local");
      EClass derived = EcoreFactory.eINSTANCE.createEClass();
      derived.setName("Derived");
      derived.getESuperTypes().add(local);
      derived.getESuperTypes().add(base);
      last.getEClassifiers().add(local);
      last.getEClassifiers().add(derived);

      view.registerRoot(other, modelUri("other"));
      view.registerRoot(root, modelUri("seeded"));
      view.commitChanges();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private org.eclipse.emf.common.util.URI modelUri(String name) {
    return org.eclipse.emf.common.util.URI.createFileURI(
        serverDir.resolve(name + "." + EXTENSION).toString());
  }

  private static List<EPackage> packages(ConstraintCheckContext context) {
    List<EPackage> result = new ArrayList<>();
    for (Resource resource : context.getModelResources()) {
      resource
          .getAllContents()
          .forEachRemaining(
              object -> {
                if (object instanceof EPackage ePackage) {
                  result.add(ePackage);
                }
              });
    }
    return result;
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  /** Package names must start with a lowercase letter. */
  private static final class LowercaseChecker implements ConstraintChecker {
    private static final ConstraintDescriptor DESCRIPTOR =
        new ConstraintDescriptor(
            LOWERCASE,
            LOWERCASE,
            "ecore::EPackage",
            ConstraintKind.INVARIANT,
            ConstraintSeverity.CRITICAL,
            "test",
            "name starts lowercase",
            Set.of("other"));

    @Override
    public List<ConstraintDescriptor> getConstraints() {
      return List.of(DESCRIPTOR);
    }

    @Override
    public List<ConstraintResult> check(
        ConstraintCheckContext context, Collection<String> constraintIds) {
      List<ConstraintViolation> violations =
          packages(context).stream()
              .filter(p -> Character.isUpperCase(p.getName().charAt(0)))
              .map(
                  p ->
                      new ConstraintViolation(
                          p, ConstraintSeverity.CRITICAL, p.getName() + " must be lowercase"))
              .toList();
      return List.of(ConstraintResult.of(DESCRIPTOR, violations));
    }
  }

  /** The virtual model must contain packages. */
  private static final class HasPackagesChecker implements ConstraintChecker {
    private static final ConstraintDescriptor DESCRIPTOR =
        new ConstraintDescriptor(HAS_PACKAGES, null, null, null, null, "test", null);

    @Override
    public List<ConstraintDescriptor> getConstraints() {
      return List.of(DESCRIPTOR);
    }

    @Override
    public List<ConstraintResult> check(
        ConstraintCheckContext context, Collection<String> constraintIds) {
      return List.of(
          packages(context).isEmpty()
              ? ConstraintResult.violated(DESCRIPTOR, List.of())
              : ConstraintResult.satisfied(DESCRIPTOR));
    }
  }
}
