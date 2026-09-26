package vitruv.tools.framework.remote.vsumprovider;

import tools.vitruv.change.interaction.InteractionResultProvider;

import java.nio.file.Path;

public interface VsumProvider {
  String getMetamodelName();

  VirtualModelInitializer getInitializer(Path storagePath);

  /**
   * Server-side initialization with a caller-supplied interaction provider
   * (e.g. {@code VitruviusInteractionResultProvider} on VitruviusServer).
   */
  default VirtualModelInitializer getInitializer(
      Path storagePath, InteractionResultProvider interactionResultProvider) {
    return getInitializer(storagePath);
  }
}
