package tools.vitruv.framework.remote.server.rest.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.interaction.UserInteractionFactory;
import tools.vitruv.framework.remote.client.exception.BadServerResponseException;
import tools.vitruv.framework.remote.client.impl.VitruvRemoteConnection;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.common.rest.models.MetamodelDto;
import tools.vitruv.framework.remote.common.rest.models.ModelDto;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;

/** Round trip through a real {@link VitruvServer} for reading models and metamodels. */
class ModelEndpointsTest {
  private static final String EXTENSION = "ecore";
  private static final String SEEDED = "/seeded.ecore";
  private static final String OTHER = "/other.ecore";

  private static boolean registeredFactory;

  @TempDir Path serverDir;
  @TempDir Path clientDir;

  private VitruvServer server;
  private VitruvRemoteConnection client;
  private HttpClient http;
  private String baseUrl;
  private VirtualModel virtualModel;

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
                virtualModel =
                    new VirtualModelBuilder()
                        .withStorageFolder(serverDir)
                        .withUserInteractorForResultProvider(
                            UserInteractionFactory.instance
                                .createPredefinedInteractionResultProvider(null))
                        .buildAndInitialize();
                seed(virtualModel);
                return virtualModel;
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
  void listsTheModelsWithTheirMetamodels() {
    List<ModelDto> models = client.getModelDescriptors();

    assertEquals(List.of(OTHER, SEEDED), models.stream().map(ModelDto::id).toList());
    MetamodelDto ecore = new MetamodelDto("ecore", EcorePackage.eNS_URI);
    assertEquals(new ModelDto(OTHER, List.of(ecore), 1), models.get(0));
    assertEquals(new ModelDto(SEEDED, List.of(ecore), 1), models.get(1));
  }

  @Test
  void returnsOneModelAsJsonWithUuids() throws Exception {
    HttpResponse<String> response = get(EndpointPath.model(SEEDED), null);

    assertEquals(200, response.statusCode());
    JsonNode json = new ObjectMapper().readTree(response.body());
    assertEquals(SEEDED, json.get("uri").asText());
    List<String> ids = new ArrayList<>();
    json.get("content").findValues("_id").forEach(id -> ids.add(id.asText()));
    assertTrue(ids.contains(uuidOf("seeded")), "the root is identified by its UUID");
  }

  @Test
  void returnsOneModelAsXmiWithUuidsAsXmiIds() throws Exception {
    String xmi = client.getModelXmi(SEEDED);

    XMLResource resource = load(xmi, "seeded.ecore");
    EPackage root = (EPackage) resource.getContents().get(0);
    assertEquals("seeded", root.getName());
    assertEquals(uuidOf("seeded"), resource.getID(root));
    // the reference to the class of the other model is written with its UUID
    String baseUuid = uuidOf("base", "Base");
    assertTrue(xmi.contains("other.ecore#" + baseUuid), xmi);
  }

  @Test
  void returnsSeveralModelsFromTheSameState() {
    ResourceSet models = client.getModelContents(null);

    assertEquals(2, models.getResources().size());
    Resource seeded =
        models.getResources().stream()
            .filter(r -> r.getURI().lastSegment().equals("seeded.ecore"))
            .findFirst()
            .orElseThrow();
    EPackage root = (EPackage) seeded.getEObject(uuidOf("seeded"));
    assertNotNull(root);
    assertEquals("seeded", root.getName());
    assertEquals(1, client.getModelContents(List.of(OTHER)).getResources().size());
  }

  @Test
  void rejectsUnknownModels() throws Exception {
    assertEquals(404, get(EndpointPath.model("/unknown.ecore"), null).statusCode());
    BadServerResponseException batch =
        assertThrows(
            BadServerResponseException.class,
            () -> client.getModelContents(List.of("/unknown.ecore")));
    assertEquals(404, batch.getStatusCode());
    assertEquals(404, get("/vsum/models/a/b", null).statusCode());
  }

  @Test
  void listsAndReturnsTheMetamodels() {
    assertEquals(
        List.of(new MetamodelDto("ecore", EcorePackage.eNS_URI)), client.getMetamodelDescriptors());

    String xmi = client.getMetamodelXmi(EcorePackage.eNS_URI);

    EPackage ecore = (EPackage) load(xmi, "ecore.ecore").getContents().get(0);
    assertEquals(EcorePackage.eNS_URI, ecore.getNsURI());
    assertNotNull(ecore.getEClassifier("EClass"));
    BadServerResponseException unknown =
        assertThrows(BadServerResponseException.class, () -> client.getMetamodelXmi("x:/y"));
    assertEquals(404, unknown.getStatusCode());
  }

  @Test
  void jsonIsTheDefaultFormat() throws Exception {
    HttpResponse<String> response = get(EndpointPath.model(SEEDED), "application/json");

    assertTrue(response.headers().firstValue("Content-Type").orElseThrow().contains("json"));
  }

  private String uuidOf(String... names) {
    return virtualModel.readModels(
        access -> {
          for (Resource resource : access.getModelResources()) {
            EPackage ePackage = (EPackage) resource.getContents().get(0);
            if (!ePackage.getName().equals(names[0])) {
              continue;
            }
            EObject element = names.length == 1 ? ePackage : ePackage.getEClassifier(names[1]);
            return access.getUuid(element).orElseThrow().getRawValue();
          }
          throw new IllegalArgumentException("unknown element");
        });
  }

  private static XMLResource load(String xmi, String fileName) {
    ResourceSet resourceSet = new ResourceSetImpl();
    XMIResourceImpl resource =
        new XMIResourceImpl(org.eclipse.emf.common.util.URI.createURI("test:/" + fileName));
    resourceSet.getResources().add(resource);
    try {
      resource.load(new ByteArrayInputStream(xmi.getBytes(StandardCharsets.UTF_8)), Map.of());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return resource;
  }

  private HttpResponse<String> get(String path, String accept) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
    if (accept != null) {
      request.header("Accept", accept);
    }
    return http.send(request.build(), BodyHandlers.ofString());
  }

  private void seed(VirtualModel model) {
    var selector = model.createSelector(ViewTypeFactory.createIdentityMappingViewType("seeding"));
    try (CommittableView view = selector.createView().withChangeRecordingTrait()) {
      EPackage other = EcoreFactory.eINSTANCE.createEPackage();
      other.setName("base");
      EClass base = EcoreFactory.eINSTANCE.createEClass();
      base.setName("Base");
      other.getEClassifiers().add(base);

      EPackage root = EcoreFactory.eINSTANCE.createEPackage();
      root.setName("seeded");
      EClass derived = EcoreFactory.eINSTANCE.createEClass();
      derived.setName("Derived");
      derived.getESuperTypes().add(base);
      root.getEClassifiers().add(derived);

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

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
