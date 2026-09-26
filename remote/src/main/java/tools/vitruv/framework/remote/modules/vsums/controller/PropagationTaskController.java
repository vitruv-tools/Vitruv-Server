package tools.vitruv.framework.remote.modules.vsums.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tools.vitruv.framework.remote.modules.vsums.usecases.PropagationTaskUseCases;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenInconsistencyResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.PropagationTaskStatusResponse;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class PropagationTaskController {

  private final PropagationTaskUseCases propagationTaskUseCases;

  @GetMapping("/{taskId}")
  public PropagationTaskStatusResponse getTaskStatus(@PathVariable UUID taskId) {
    return propagationTaskUseCases.getTaskStatus(taskId);
  }

  @PostMapping("/{taskId}/interaction")
  @ResponseStatus(NO_CONTENT)
  public void submitInteraction(@PathVariable UUID taskId, @RequestBody String interactionJson) {
    propagationTaskUseCases.submitInteraction(taskId, interactionJson);
  }

  /**
   * Parks a waiting interaction as the open inconsistency for its VSUM.
   * Does not abort the worker; resolve later via POST /interaction from the hub.
   */
  @PostMapping("/{taskId}/inconsistent")
  public OpenInconsistencyResponse parkAsInconsistency(@PathVariable UUID taskId) {
    return propagationTaskUseCases.parkAsInconsistency(taskId);
  }
}
