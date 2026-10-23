package tools.vitruv.framework.remote.client.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.composite.description.TransactionalChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.VitruviusChangeFactory;
import tools.vitruv.change.composite.description.VitruviusChangeResolverFactory;
import tools.vitruv.change.composite.recording.ChangeRecorder;
import tools.vitruv.change.interaction.UserInteractionBase;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewSelection;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewType;
import tools.vitruv.framework.views.changederivation.StateBasedChangeResolutionStrategy;

/**
 * A {@link RemoteView} that records changes to its resources and allows to propagate them back to
 * the Vitruvius server using the {@link #commitChanges} method.
 */
public class ChangeRecordingRemoteView implements CommittableView {
  private final RemoteView base;
  private ChangeRecorder changeRecorder;

  // Changes of earlier commits that the server did not take, sent first by the next commit.
  private VitruviusChange<HierarchicalId> unsentChange;

  /**
   * Creates a new {@link ChangeRecordingRemoteView} based on the given {@link RemoteView}.
   *
   * @param base the base remote view
   */
  public ChangeRecordingRemoteView(RemoteView base) {
    checkArgument(base != null, "base must not be null");
    checkState(!base.isModified(), "view must not be modified");
    this.base = base;
    setupChangeRecorder();
  }

  private void setupChangeRecorder() {
    changeRecorder = new ChangeRecorder(base.viewSource);
    changeRecorder.addToRecording(base.viewSource);
    changeRecorder.beginRecording();
  }

  @Override
  public void close() {
    if (!isClosed()) {
      changeRecorder.close();
    }
    base.close();
  }

  @Override
  public Collection<EObject> getRootObjects() {
    return base.getRootObjects();
  }

  @Override
  public boolean isModified() {
    return base.isModified();
  }

  @Override
  public boolean isOutdated() {
    return base.isOutdated();
  }

  @Override
  public void update() {
    if (changeRecorder.isRecording()) {
      changeRecorder.endRecording();
    }
    changeRecorder.close();
    base.update();
    setupChangeRecorder();
  }

  @Override
  public boolean isClosed() {
    return base.isClosed();
  }

  @Override
  public void registerRoot(EObject object, URI persistAt) {
    base.registerRoot(object, persistAt);
  }

  @Override
  public void moveRoot(EObject object, URI newLocation) {
    base.moveRoot(object, newLocation);
  }

  @Override
  public ViewSelection getSelection() {
    return base.getSelection();
  }

  @Override
  public ViewType<? extends ViewSelector> getViewType() {
    return base.getViewType();
  }

  @Override
  public CommittableView withChangeRecordingTrait() {
    changeRecorder.close();
    return base.withChangeDerivingTrait();
  }

  @Override
  public CommittableView withChangeDerivingTrait(
      StateBasedChangeResolutionStrategy changeResolutionStrategy) {
    changeRecorder.close();
    return base.withChangeDerivingTrait(changeResolutionStrategy);
  }

  /**
   * Commits the changes made to the view and its containing elements. If the server does not take
   * them, e.g., because it answers 409 while a change committed asynchronously before waits for
   * user input, the view stays modified and keeps the changes: the next commit, synchronous or
   * asynchronous, sends them along with the changes made in the meantime.
   *
   * @throws IllegalStateException if called on a closed view
   * @throws tools.vitruv.framework.remote.client.exception.BadServerResponseException if the server
   *     does not take the changes
   * @see #isClosed()
   * @see #commitChangesAndUpdate()
   */
  @Override
  public void commitChanges() {
    commit(
        null,
        change -> {
          base.remoteConnection.propagateChanges(base.uuid, change);
          return null;
        });
  }

  /**
   * Commits the changes made to the view and its containing elements, along with the provided user
   * inputs. Changes the server does not take are kept like in {@link #commitChanges()}.
   *
   * @param userInputs the user interactions associated with the changes
   */
  public void commitChanges(Iterable<UserInteractionBase> userInputs) {
    commit(
        userInputs,
        change -> {
          base.remoteConnection.propagateChanges(base.uuid, change);
          return null;
        });
  }

  /**
   * Commits the changes made to the view like {@link #commitChanges()}, but lets the server
   * propagate them asynchronously: the server answers as soon as it has taken the changes down, and
   * a change waiting for user input does not hold up the changes of other views. The changes of
   * this view are propagated in the order they are committed, so later commits wait for this one;
   * while it waits for user input, {@link #commitChanges()} is rejected with status code 409.
   * The changes of an earlier commit that the server rejected are sent along with this one. Follow
   * the propagation with {@link
   * tools.vitruv.framework.remote.client.VitruvClient#getTaskStatus(int)} and update the view once
   * it is {@link tools.vitruv.framework.remote.common.rest.TaskState#DONE}.
   *
   * @return the task id the server assigned to the changes
   * @throws IllegalStateException if called on a closed view
   */
  public int commitChangesAsync() {
    return commit(null, change -> base.remoteConnection.propagateChangesAsync(base.uuid, change));
  }

  /**
   * Sends the changes recorded since the last commit, preceded by those of earlier commits that the
   * server did not take. If sending fails, all of them are kept for the next commit. Either way,
   * recording goes on afterwards, so the view can still be committed.
   */
  private <T> T commit(
      Iterable<UserInteractionBase> userInputs, Function<VitruviusChange<HierarchicalId>, T> send) {
    base.checkNotClosed();
    var recordedChange = changeRecorder.endRecording();
    try {
      var changeResolver = VitruviusChangeResolverFactory.forHierarchicalIds(base.viewSource);
      VitruviusChange<HierarchicalId> change = changeResolver.assignIds(recordedChange);
      if (userInputs != null) {
        ((TransactionalChange<?>) change).setUserInteractions(userInputs);
      }
      if (unsentChange != null) {
        // The ids of the earlier changes refer to the state before the new ones, so they come
        // first.
        change =
            VitruviusChangeFactory.getInstance()
                .createCompositeChange(List.of(unsentChange, change));
      }
      unsentChange = change;
      T result = send.apply(change);
      unsentChange = null;
      base.modified = false;
      return result;
    } finally {
      changeRecorder.beginRecording();
    }
  }
}
