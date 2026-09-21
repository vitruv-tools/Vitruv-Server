package tools.vitruv.framework.remote.modules.vsums.controller;

import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.InconsistencyVisualizationUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.CreateInconsistencyCommentRequest;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyCommentResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyContextResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.InconsistencyModelSnapshotResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenInconsistencyResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.RecordInconsistencyResolutionRequest;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.ViewUpdateSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/inconsistencies")
@RequiredArgsConstructor
public class InconsistencyController {

    private final InconsistencyUseCases inconsistencyUseCases;
    private final InconsistencyVisualizationUseCases inconsistencyVisualizationUseCases;

    @GetMapping
    public List<OpenInconsistencyResponse> list(
            @RequestParam(required = false) String state
    ) {
        return inconsistencyUseCases.list(state);
    }

    @GetMapping("/{id}")
    public OpenInconsistencyResponse get(@PathVariable UUID id) {
        return inconsistencyUseCases.get(id);
    }

    @GetMapping("/{id}/context")
    public InconsistencyContextResponse getContext(@PathVariable UUID id) {
        return inconsistencyVisualizationUseCases.getContext(id);
    }

    @GetMapping("/{id}/model")
    public InconsistencyModelSnapshotResponse getModel(@PathVariable UUID id) {
        return inconsistencyUseCases.getModelSnapshot(id);
    }

    @GetMapping("/{id}/comments")
    public List<InconsistencyCommentResponse> listComments(@PathVariable UUID id) {
        return inconsistencyUseCases.listComments(id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public InconsistencyCommentResponse addComment(
            @PathVariable UUID id,
            @RequestBody CreateInconsistencyCommentRequest request
    ) {
        return inconsistencyUseCases.addComment(id, request);
    }

    @PostMapping("/{id}/resolution")
    public OpenInconsistencyResponse recordResolution(
            @PathVariable UUID id,
            @RequestBody RecordInconsistencyResolutionRequest request
    ) {
        return inconsistencyUseCases.recordResolution(id, request);
    }

    @GetMapping("/{id}/view-updates")
    public List<ViewUpdateSummaryResponse> listViewUpdates(@PathVariable UUID id) {
        return inconsistencyUseCases.listViewUpdates(id);
    }
}
