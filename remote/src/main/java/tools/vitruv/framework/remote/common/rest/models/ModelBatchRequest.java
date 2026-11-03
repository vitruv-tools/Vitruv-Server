package tools.vitruv.framework.remote.common.rest.models;

import java.util.List;

/**
 * Requests the content of several models at once.
 *
 * @param ids the ids of the models, see {@link ModelDto#id()}; {@code null} for all models
 */
public record ModelBatchRequest(List<String> ids) {}
