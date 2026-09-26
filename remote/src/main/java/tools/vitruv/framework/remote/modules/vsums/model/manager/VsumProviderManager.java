package tools.vitruv.framework.remote.modules.vsums.model.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.vitruv.framework.remote.modules.vsums.model.services.VsumProviderLoader;
import tools.vitruv.framework.remote.vsumprovider.VsumProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle and retrieval of VsumProvider instances. This class is responsible for
 * caching and dynamically loading VsumProvider implementations as required, based on the associated
 * metamodel names.
 * <p>
 * It internally relies on a VsumProviderLoader to dynamically load all available VsumProvider
 * implementations from a predefined source. Once loaded, the instances are cached for efficient future access.
 */
@Component
@RequiredArgsConstructor
public class VsumProviderManager {
  private final VsumProviderLoader vsumProviderLoader;

  /**
   * metamodelName -> vsumProvider
   */
  private final Map<String, VsumProvider> vsumProviders = new ConcurrentHashMap<>();

  /**
   * Retrieves the VsumProvider associated with the given metamodel name.
   * If the provider is not already cached, it will trigger a loading of metamodels
   * to ensure the requested provider is available.
   *
   * @param metamodelName the name of the metamodel for which the VsumProvider is requested
   * @return the VsumProvider corresponding to the specified metamodel name, or null if no such provider exists
   */
  public VsumProvider getVsumProvider(String metamodelName) {
    var cachedProvider = vsumProviders.get(metamodelName);
    if (cachedProvider != null) {
      return cachedProvider;
    }
    loadMetamodels();
    return vsumProviders.get(metamodelName);
  }

  /**
   * Retrieves the names of all currently available metamodels.
   * This method ensures that all metamodels are loaded before returning their names.
   *
   * @return an array of strings, where each string represents the name of a metamodel
   */
  public String[] getMetamodelNames() {
    loadMetamodels();
    return vsumProviders.keySet().toArray(String[]::new);
  }

  private void loadMetamodels() {
    List<VsumProvider> vsumProviders;
    try {
      vsumProviders = vsumProviderLoader.loadAll();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    this.vsumProviders.clear();
    vsumProviders.forEach(provider -> this.vsumProviders.put(provider.getMetamodelName(), provider));
  }
}
