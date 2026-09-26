package tools.vitruv.framework.remote.modules.vsums.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.vitruv.framework.remote.modules.vsums.model.manager.VsumProviderManager;
import tools.vitruv.framework.remote.modules.vsums.model.services.EcoreModelLoader;

/**
 * REST controller that provides endpoints for managing and retrieving information about metamodels.
 * It offers methods to list all available metamodels and to fetch Ecore models for a specific metamodel.
 */
@RestController
@RequestMapping("/v1/metamodels")
@RequiredArgsConstructor
public class MetamodelController {
  private final VsumProviderManager vsumProviderManager;
  private final EcoreModelLoader ecoreModelLoader;

  /**
   * Retrieves the names of all currently available metamodels.
   *
   * @return an array of strings, where each string represents the name of a metamodel
   */
  @GetMapping
  public String[] getMetamodelNames() {
    return vsumProviderManager.getMetamodelNames();
  }

  /**
   * Retrieves the Ecore models for a specified metamodel.
   *
   * @param name the name of the metamodel for which the Ecore models should be retrieved
   * @return a JSON array string containing the processed Ecore models for the specified metamodel
   */
  @GetMapping(value = "/{name}/ecore-models", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getEcoreModels(@PathVariable String name) {
    return ecoreModelLoader.loadEcoreModels(name);
  }
}
