package tools.vitruv.framework.remote.common.rest.models;

import tools.vitruv.framework.vsum.models.MetamodelReference;

/**
 * A metamodel of the virtual model, identified by its root package.
 *
 * @param name the name of the root package, as used in qualified type names
 * @param nsUri the namespace URI of the root package, which identifies the metamodel
 */
public record MetamodelDto(String name, String nsUri) {

  /**
   * Creates the transfer object for the given metamodel.
   *
   * @param metamodel the metamodel
   * @return the transfer object
   */
  public static MetamodelDto from(MetamodelReference metamodel) {
    return new MetamodelDto(metamodel.name(), metamodel.nsUri());
  }
}
