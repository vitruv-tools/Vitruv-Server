package tools.vitruv.framework.remote.client.impl;

import org.eclipse.emf.ecore.EPackage;
import tools.vitruv.framework.views.ChangeableViewSource;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewType;

/**
 * A Vitruvius view type representing actual types on the virtual model, 
 * but is still capable of providing a view selector and allows creating
 * views by querying the Vitruvius server.
 */
public class RemoteViewType implements ViewType<ViewSelector> {
  private final String name;
  private final VitruvRemoteConnection remoteConnection;
  private EPackage metamodel;

  /**
   * Creates a new view type.
   *
   * @param name - String
   * @param remoteConnection - {@link VitruvRemoteConnection}
   */
  RemoteViewType(String name, VitruvRemoteConnection remoteConnection) {
    this.name = name;
    this.remoteConnection = remoteConnection;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Returns the {@link ViewSelector} of the {@link ViewType}, 
   * which allows configuring views by delegating the request to the Vitruvius server.
   *
   * @param viewSource Ignored, can be null.
   * @return A view selector for the view type represented by this remote view type.
   */
  @Override
  public ViewSelector createSelector(ChangeableViewSource viewSource) {
    return remoteConnection.getSelector(name);
  }

  /**
   * Returns the metamodel (i.e. actual view type) as {@link EPackage},
   * by delegating the requrest to the Vitruvius server.
   * When metamodel has already been loaded, returns it instead.
   *
   * @return {@link EPackage}
   */
  @Override
  public EPackage getMetamodel() {
    if (metamodel == null) {
      metamodel = remoteConnection.getMetamodel(name);
    }
    return metamodel;
  }
}
