package tools.vitruv.framework.remote.modules.vsums.usecases;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.vitruv.change.interaction.MultipleChoiceMultiSelectionUserInteraction;
import tools.vitruv.change.interaction.MultipleChoiceSingleSelectionUserInteraction;
import tools.vitruv.change.interaction.UserInteractionBase;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskState;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskStatus;
import tools.vitruv.framework.remote.modules.vsums.async.PropagationTaskRegistry;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistency;
import tools.vitruv.framework.remote.modules.vsums.model.entities.OpenInconsistencyRepo;
import tools.vitruv.framework.remote.modules.vsums.model.manager.ViewManager;
import tools.vitruv.framework.remote.modules.vsums.model.services.ViewService;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.ViewWrapper;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.OpenInconsistencyResponse;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.PropagationTaskStatusResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropagationTaskUseCases {

  private final PropagationTaskRegistry taskRegistry;
  private final ViewManager viewManager;
  private final ViewService viewService;
  private final InconsistencyUseCases inconsistencyUseCases;
  private final OpenInconsistencyRepo openInconsistencyRepo;
  private final InconsistencyModelSnapshotEnricher modelSnapshotEnricher;

  public PropagationTaskStatusResponse getTaskStatus(UUID taskId) {
    AsyncTaskStatus status = getTaskOrThrow(taskId);
    return toResponse(status);
  }

  public void submitInteraction(UUID taskId, String interactionJson) {
    AsyncTaskStatus status = getTaskOrThrow(taskId);

    if (status.getState() != AsyncTaskState.WAITING_USER_INTERACTION) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Task with ID '" + taskId + "' is not waiting for user interaction");
    }

    val viewWrapper = viewManager.getView(status.getViewId());
    if (viewWrapper == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "View not found for task: " + taskId);
    }

    UserInteractionBase response;
    try {
      response = viewWrapper.vsumWrapper().jsonMapper().deserialize(interactionJson, UserInteractionBase.class);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interaction JSON: " + e.getMessage());
    }

    status.setInteractionResponse(response);
  }

  /**
   * Parks the waiting task as the single open inconsistency for its VSUM.
   * Worker stays blocked on the prompt so the hub can resolve it later.
   */
  public OpenInconsistencyResponse parkAsInconsistency(UUID taskId) {
    AsyncTaskStatus status = getTaskOrThrow(taskId);

    if (status.getState() != AsyncTaskState.WAITING_USER_INTERACTION) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Task with ID '" + taskId + "' is not waiting for user interaction");
    }

    ViewWrapper viewWrapper = viewManager.getView(status.getViewId());
    if (viewWrapper == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "View not found for task: " + taskId);
    }

    Map<String, Object> interactionPayload = status.getPendingInteraction() != null
        ? serializeInteraction(status.getPendingInteraction())
        : Map.of();

    String message = status.getPendingInteraction() != null
        ? status.getPendingInteraction().getMessage()
        : null;

    OpenInconsistency created = inconsistencyUseCases.createOpen(
        viewWrapper.vsumWrapper().info().getId(),
        viewWrapper.vsumWrapper().info().getName(),
        taskId,
        status.getViewId(),
        message != null ? message : "Pending user interaction",
        message,
        interactionPayload
    );

    try {
      String encoded = status.getCommittedResourceSet();
      if (encoded == null || encoded.isBlank()) {
        encoded = viewService.serializeResourceSet(viewWrapper);
      }
      if (encoded != null && !encoded.isBlank()) {
        encoded = modelSnapshotEnricher.enrich(encoded, message);
        created.setModelSnapshotEncodedResourceSet(encoded);
        created.setModelSnapshotViewTypeName(viewWrapper.view().getViewType().getName());
        openInconsistencyRepo.save(created);
        viewService.saveViewSnapshot(viewWrapper);
      }
    } catch (RuntimeException e) {
      // Hub visualization can still fall back to the open view while parked.
    }

    taskRegistry.parkAsInconsistency(taskId);
    return inconsistencyUseCases.get(created.getId());
  }

  private AsyncTaskStatus getTaskOrThrow(UUID taskId) {
    AsyncTaskStatus status = taskRegistry.getTaskStatus(taskId);
    if (status == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task with ID '" + taskId + "' not found");
    }
    return status;
  }

  private PropagationTaskStatusResponse toResponse(AsyncTaskStatus status) {
    Map<String, Object> interactionPayload = null;
    String resultJson = null;

    val viewWrapper = viewManager.getView(status.getViewId());
    if (viewWrapper != null) {
      if (status.isWaitingForUserInteraction() && status.getPendingInteraction() != null) {
        interactionPayload = serializeInteraction(status.getPendingInteraction());
        if (status.getValidationError() != null) {
          interactionPayload.put("validationError", status.getValidationError());
        }
      } else if (status.getState() == AsyncTaskState.INCONSISTENT && status.getPendingInteraction() != null) {
        interactionPayload = serializeInteraction(status.getPendingInteraction());
      }
      if (status.getState() == AsyncTaskState.COMPLETED && status.getResult() != null) {
        resultJson = status.getResult();
      }
    }

    return new PropagationTaskStatusResponse(
        status.getTaskId(),
        status.getViewId(),
        status.getState(),
        status.getCreatedAt(),
        status.getCompletedAt(),
        status.getErrorMessage(),
        interactionPayload,
        resultJson
    );
  }

  private Map<String, Object> serializeInteraction(UserInteractionBase interaction) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put(
        "eClass",
        interaction.eClass().getEPackage().getNsURI() + "#//" + interaction.eClass().getName());
    if (interaction.getMessage() != null) {
      payload.put("message", interaction.getMessage());
    }
    if (interaction instanceof MultipleChoiceSingleSelectionUserInteraction singleSelection) {
      payload.put("choices", new ArrayList<>(singleSelection.getChoices()));
    } else if (interaction instanceof MultipleChoiceMultiSelectionUserInteraction multiSelection) {
      payload.put("choices", new ArrayList<>(multiSelection.getChoices()));
    }
    return payload;
  }
}
