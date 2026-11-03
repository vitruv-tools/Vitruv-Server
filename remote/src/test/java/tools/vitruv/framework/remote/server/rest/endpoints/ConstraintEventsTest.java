package tools.vitruv.framework.remote.server.rest.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
import tools.vitruv.framework.remote.client.Subscription;
import tools.vitruv.framework.remote.client.impl.VitruvRemoteConnection;
import tools.vitruv.framework.remote.common.rest.constraints.ConstraintStatusDto;
import tools.vitruv.framework.remote.server.VitruvServer;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.constraints.ConstraintCheckContext;
import tools.vitruv.framework.vsum.constraints.ConstraintChecker;
import tools.vitruv.framework.vsum.constraints.ConstraintDescriptor;
import tools.vitruv.framework.vsum.constraints.ConstraintResult;
import tools.vitruv.framework.vsum.constraints.ConstraintSeverity;
import tools.vitruv.framework.vsum.constraints.ConstraintViolation;

/** Subscribing to the results of constraint validations through a real {@link VitruvServer}. */
class ConstraintEventsTest {
  private static final String EXTENSION = "ecore";
  private static final String LOWERCASE = "lowercasePackageNames";
  private static final long TIMEOUT_SECONDS = 10;

  private static boolean registeredFactory;

  @TempDir Path serverDir;
  @TempDir Path clientDir;

  private VitruvServer server;
  private VitruvRemoteConnection client;
  private String baseUrl;
  private VirtualModel virtualModel;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private boolean stopped;

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
                        .withConstraintChecker(new LowercaseChecker())
                        .buildAndInitialize();
                addPackage(virtualModel, "good");
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
  }

  @AfterEach
  void stopServer() {
    subscriptions.forEach(Subscription::close);
    if (!stopped) {
      server.stop();
    }
  }

  @Test
  void sendsTheCurrentStatusAndTheStatusAfterEveryChange() throws Exception {
    BlockingQueue<ConstraintStatusDto> events = new LinkedBlockingQueue<>();
    subscriptions.add(client.subscribeConstraintEvents(events::add));

    ConstraintStatusDto initial = next(events);
    assertTrue(initial.satisfied());
    assertEquals("SATISFIED", initial.constraints().get(LOWERCASE));
    assertEquals(client.getConstraintRevision(), initial.revision());
    assertTrue(virtualModel.isConstraintValidationAfterPropagation());

    addPackage(virtualModel, "Bad");

    ConstraintStatusDto afterChange = next(events);
    assertFalse(afterChange.satisfied());
    assertEquals("VIOLATED", afterChange.constraints().get(LOWERCASE));
  }

  @Test
  void sendsServerSentEvents() throws Exception {
    HttpResponse<InputStream> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/vsum/constraints/events"))
                    .GET()
                    .build(),
                BodyHandlers.ofInputStream());

    assertEquals(200, response.statusCode());
    assertEquals(
        "text/event-stream", response.headers().firstValue("Content-Type").orElseThrow());
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
      assertEquals("event: validation", reader.readLine());
      assertTrue(reader.readLine().startsWith("id: "));
      String data = reader.readLine();
      assertTrue(data.startsWith("data: "), data);
    }
  }

  @Test
  void notifiesEverySubscriber() throws Exception {
    BlockingQueue<ConstraintStatusDto> first = new LinkedBlockingQueue<>();
    BlockingQueue<ConstraintStatusDto> second = new LinkedBlockingQueue<>();
    subscriptions.add(client.subscribeConstraintEvents(first::add));
    next(first);
    subscriptions.add(client.subscribeConstraintEvents(second::add));
    next(second);
    first.clear();

    addPackage(virtualModel, "Bad");

    assertFalse(lastOf(first).satisfied());
    assertFalse(lastOf(second).satisfied());
  }

  @Test
  void restoresTheValidationSettingAndClosesSubscriptionsWhenStopping() throws Exception {
    BlockingQueue<ConstraintStatusDto> events = new LinkedBlockingQueue<>();
    final Subscription subscription = client.subscribeConstraintEvents(events::add);
    next(events);
    assertTrue(virtualModel.isConstraintValidationAfterPropagation());

    server.stop();

    assertFalse(virtualModel.isConstraintValidationAfterPropagation());
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
    while (subscription.isOpen() && System.currentTimeMillis() < deadline) {
      Thread.sleep(20);
    }
    assertFalse(subscription.isOpen(), "the server ended the subscription");
    stopped = true;
  }

  private static ConstraintStatusDto next(BlockingQueue<ConstraintStatusDto> events)
      throws InterruptedException {
    ConstraintStatusDto event = events.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertNotNull(event, "no event received");
    return event;
  }

  /** Waits for the event of the last change; the initial validation may still arrive first. */
  private static ConstraintStatusDto lastOf(BlockingQueue<ConstraintStatusDto> events)
      throws InterruptedException {
    ConstraintStatusDto event = next(events);
    while (event.satisfied()) {
      event = next(events);
    }
    return event;
  }

  private void addPackage(VirtualModel model, String name) {
    var selector = model.createSelector(ViewTypeFactory.createIdentityMappingViewType("seeding"));
    try (CommittableView view = selector.createView().withChangeRecordingTrait()) {
      EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
      ePackage.setName(name);
      view.registerRoot(
          ePackage,
          org.eclipse.emf.common.util.URI.createFileURI(
              serverDir.resolve(name + "." + EXTENSION).toString()));
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

  /** Package names must start with a lowercase letter. */
  private static final class LowercaseChecker implements ConstraintChecker {
    private static final ConstraintDescriptor DESCRIPTOR =
        new ConstraintDescriptor(
            LOWERCASE, null, "ecore::EPackage", null, ConstraintSeverity.CRITICAL, "test", null);

    @Override
    public List<ConstraintDescriptor> getConstraints() {
      return List.of(DESCRIPTOR);
    }

    @Override
    public List<ConstraintResult> check(
        ConstraintCheckContext context, Collection<String> constraintIds) {
      List<ConstraintViolation> violations = new ArrayList<>();
      for (Resource resource : context.getModelResources()) {
        for (var element : resource.getContents()) {
          if (element instanceof EPackage ePackage
              && Character.isUpperCase(ePackage.getName().charAt(0))) {
            violations.add(
                new ConstraintViolation(ePackage, ConstraintSeverity.CRITICAL, "uppercase"));
          }
        }
      }
      return List.of(ConstraintResult.of(DESCRIPTOR, violations));
    }
  }
}
