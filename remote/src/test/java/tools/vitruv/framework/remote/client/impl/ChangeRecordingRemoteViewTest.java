package tools.vitruv.framework.remote.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.util.List;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.vitruv.change.composite.description.CompositeChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.framework.remote.client.exception.BadServerResponseException;
import tools.vitruv.framework.views.ViewSelector;

/** Tests what a {@link ChangeRecordingRemoteView} sends, with a mocked connection to the server. */
class ChangeRecordingRemoteViewTest {
  private static final String VIEW_UUID = "change-recording-remote-view-test-view";

  private final VitruvRemoteConnection connection = mock(VitruvRemoteConnection.class);
  private EPackage root;
  private ChangeRecordingRemoteView view;

  @BeforeEach
  void createView() {
    ResourceSet viewSource = new ResourceSetImpl();
    Resource resource = new ResourceImpl(URI.createURI("test:/view.ecore"));
    viewSource.getResources().add(resource);
    root = EcoreFactory.eINSTANCE.createEPackage();
    root.setName("root");
    resource.getContents().add(root);
    view =
        new ChangeRecordingRemoteView(
            new RemoteView(
                VIEW_UUID,
                viewSource,
                mock(ViewSelector.class),
                mock(RemoteViewType.class),
                connection));
  }

  @Test
  void rejectedCommit_keepsTheChangesForTheNextCommit_andRecordingGoesOn() {
    doThrow(new BadServerResponseException("waits for input", HttpURLConnection.HTTP_CONFLICT))
        .doNothing()
        .when(connection)
        .propagateChanges(eq(VIEW_UUID), any());
    root.setName("first");

    assertThrows(BadServerResponseException.class, view::commitChanges);
    assertTrue(view.isModified());

    root.setNsPrefix("second");
    view.commitChanges();

    // The second commit sends the rejected changes first, followed by those made since.
    List<VitruviusChange<?>> sent = sentChanges(2);
    CompositeChange<?, ?> resent = assertInstanceOf(CompositeChange.class, sent.get(1));
    assertEquals(2, resent.getChanges().size());
    assertSame(sent.get(0), resent.getChanges().get(0));
    assertEquals(2, resent.getEChanges().size());
    assertFalse(view.isModified());
  }

  @Test
  void changesAnAsynchronousCommitCouldNotSend_goWithTheNextCommit_andOnlyWithIt() {
    when(connection.propagateChangesAsync(eq(VIEW_UUID), any()))
        .thenThrow(
            new BadServerResponseException("unavailable", HttpURLConnection.HTTP_UNAVAILABLE));
    root.setName("first");

    assertThrows(BadServerResponseException.class, view::commitChangesAsync);
    view.commitChanges();
    root.setNsPrefix("second");
    view.commitChanges();

    List<VitruviusChange<?>> sent = sentChanges(2);
    assertEquals(1, sent.get(0).getEChanges().size());
    assertEquals(1, sent.get(1).getEChanges().size());
    assertFalse(sent.get(1) instanceof CompositeChange, "the kept change was sent again");
    assertFalse(view.isModified());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private List<VitruviusChange<?>> sentChanges(int expectedCommits) {
    ArgumentCaptor<VitruviusChange<?>> captor =
        ArgumentCaptor.forClass((Class) VitruviusChange.class);
    verify(connection, times(expectedCommits)).propagateChanges(eq(VIEW_UUID), captor.capture());
    return captor.getAllValues();
  }
}
