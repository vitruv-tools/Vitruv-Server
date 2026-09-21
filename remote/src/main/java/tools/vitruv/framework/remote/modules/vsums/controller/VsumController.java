package tools.vitruv.framework.remote.modules.vsums.controller;

import tools.vitruv.framework.remote.modules.vsums.usecases.ViewUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.VsumInfoUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.VsumUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.CreateVsumRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.UpdateVsumInfoRequestBody;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.VsumInfoResponseBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller class for managing VSUM entities and their associated operations.
 * Provides endpoints for retrieving, creating, updating, and deleting VSUM
 * information records, as well as handling view types and selectors.
 * <p>
 * This class handles the following key functionalities:
 * - Fetching existing VSUM information records.
 * - Creating new VSUM information records.
 * - Updating existing VSUM information records.
 * - Deleting VSUM entities by their unique identifier.
 * - Retrieving available view types for specific VSUMs.
 * - Creating selectors for VSUMs based on specified view types.
 * <p>
 * The endpoints in this controller follow RESTful conventions and interact
 * with the underlying use case layer to process the business logic.
 */
@RestController
@RequestMapping("/v1/vsums")
@RequiredArgsConstructor
public class VsumController {

    private final VsumInfoUseCases vsumInfoUseCases;
    private final VsumUseCases vsumUseCases;
    private final ViewUseCases viewUseCases;


    /**
     * Retrieves an array of VSUM information records. Each record contains details such as
     * the unique identifier, metamodel name, name, and description of the VSUM.
     *
     * @return an array of {@code VsumInfoResponseBody} objects, each representing a VSUM information record
     */
    @GetMapping
    public VsumInfoResponseBody[] getVsums() {
        return vsumInfoUseCases.getVsumInfos();
    }

    /**
     * Creates a new VSUM information record based on the provided request body.
     *
     * @param body the request body containing the metamodel name, name, and description
     *             required to create the VSUM information record
     * @return the newly created VSUM information record encapsulated in a {@code VsumInfoResponseBody}
     */
    @PostMapping
    public VsumInfoResponseBody createVsum(@RequestBody CreateVsumRequestBody body) {
        return vsumInfoUseCases.createVsumInfo(body);
    }

    /**
     * Updates an existing VSUM information record identified by the provided VSUM ID
     * with the details specified in the request body.
     *
     * @param vsumId the unique identifier of the VSUM information record to be updated
     * @param body   the request body containing the updated name and description for the VSUM information record
     * @return the updated VSUM information record encapsulated in a {@code VsumInfoResponseBody}
     */
    @PutMapping("/{vsumId}")
    public VsumInfoResponseBody updateVsumInfo(@PathVariable UUID vsumId, @RequestBody UpdateVsumInfoRequestBody body) {
        return vsumInfoUseCases.updateVsumInfo(vsumId, body);
    }

    /**
     * Deletes a VSUM information record identified by the provided VSUM ID.
     *
     * @param vsumId the unique identifier of the VSUM information record to be deleted
     */
    @DeleteMapping("/{vsumId}")
    public void deleteVsum(@PathVariable UUID vsumId) {
        vsumUseCases.deleteVsum(vsumId);
    }

    /**
     * Retrieves the available view types associated with the specified VSUM ID.
     *
     * @param vsumId the unique identifier of the VSUM for which the view types are retrieved
     * @return an array of strings representing the names of the available view types for the specified VSUM
     */
    @GetMapping("/{vsumId}/view-types")
    public String[] getViewTypes(@PathVariable UUID vsumId) {
        return vsumUseCases.getViewTypeNames(vsumId);
    }

    /**
     * Creates a selector for a specified VSUM and view type.
     *
     * @param vsumId       the unique identifier of the VSUM for which the selector is being created
     * @param viewTypeName the name of the view type for which the selector is being created
     * @return the identifier of the newly created selector
     */
    @PostMapping("/{vsumId}/view-types/{viewTypeName}/selectors")
    public String createSelector(@PathVariable UUID vsumId, @PathVariable String viewTypeName) {
        return viewUseCases.createSelector(vsumId, viewTypeName);
    }
}
