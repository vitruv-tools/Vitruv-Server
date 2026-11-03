package tools.vitruv.framework.remote.common.rest.models;

import java.util.List;

/**
 * A model of the virtual model.
 *
 * @param id the id of the model: its path relative to the folder of the virtual model, e.g. {@code
 *     /model/items.shop}; equals the {@code resource} of constraint violations
 * @param metamodels the metamodels of the elements of the model
 * @param rootCount the number of root elements of the model
 */
public record ModelDto(String id, List<MetamodelDto> metamodels, int rootCount) {

  /** Makes the metamodels immutable. */
  public ModelDto {
    metamodels = metamodels == null ? List.of() : List.copyOf(metamodels);
  }
}
