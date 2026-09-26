package tools.vitruv.framework.remote.modules.vsums.usecases;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.vitruv.framework.remote.modules.vsums.async.AsyncTaskStatus;
import tools.vitruv.framework.remote.modules.vsums.async.PropagationTaskRegistry;
import tools.vitruv.framework.remote.modules.vsums.model.entities.*;
import tools.vitruv.framework.remote.modules.vsums.model.manager.ViewManager;
import tools.vitruv.framework.remote.modules.vsums.model.services.ViewService;
import tools.vitruv.framework.remote.modules.vsums.model.wrapper.ViewWrapper;
import tools.vitruv.framework.remote.modules.vsums.usecases.dtos.*;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InconsistencyUseCases {

  private final OpenInconsistencyRepo openInconsistencyRepo;
  private final InconsistencyCommentRepo inconsistencyCommentRepo;
  private final ViewUpdateRepo viewUpdateRepo;
  private final VsumInfoRepo vsumInfoRepo;
  private final ViewManager viewManager;
  private final ViewService viewService;
  private final PropagationTaskRegistry taskRegistry;
  private final InconsistencyModelSnapshotEnricher modelSnapshotEnricher;
  private final ObjectMapper objectMapper;

  /**
   * Lists inconsistencies. When {@code state} is null or blank, returns OPEN only (hub default).
   * Pass {@code ALL} for every state.
   */
  public List<OpenInconsistencyResponse> list(String state) {
    reconcileStaleOpenInconsistencies();
    if (state == null || state.isBlank() || "OPEN".equalsIgnoreCase(state)) {
      return openInconsistencyRepo.findByStateOrderByCreatedAtDesc(OpenInconsistencyState.OPEN).stream()
          .map(this::toResponse)
          .toList();
    }
    if ("ALL".equalsIgnoreCase(state)) {
      return openInconsistencyRepo.findAllByOrderByCreatedAtDesc().stream()
          .map(this::toResponse)
          .toList();
    }
    OpenInconsistencyState parsed = parseState(state);
    return openInconsistencyRepo.findByStateOrderByCreatedAtDesc(parsed).stream()
        .map(this::toResponse)
        .toList();
  }

  public List<OpenInconsistencyResponse> listOpen() {
    return list("OPEN");
  }

  public OpenInconsistencyResponse get(UUID id) {
    reconcileStaleOpenInconsistencies();
    return toResponse(getEntityOrThrow(id));
  }

  public OpenInconsistency createOpen(
      UUID vsumId,
      String vsumName,
      UUID taskId,
      UUID viewId,
      String title,
      String message,
      Map<String, Object> interaction
  ) {
    openInconsistencyRepo.findByVsumIdAndState(vsumId, OpenInconsistencyState.OPEN).ifPresent(existing -> {
      if (!existing.getTaskId().equals(taskId)) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "VSUM already has an open inconsistency (task " + existing.getTaskId() + ")");
      }
    });

    var existingForTask = openInconsistencyRepo.findByTaskIdAndState(taskId, OpenInconsistencyState.OPEN);
    if (existingForTask.isPresent()) {
      return existingForTask.get();
    }

    OpenInconsistency entity = new OpenInconsistency();
    entity.setVsumId(vsumId);
    entity.setVsumName(vsumName);
    entity.setTaskId(taskId);
    entity.setViewId(viewId);
    entity.setTitle(title != null && !title.isBlank() ? title : "Pending user interaction");
    entity.setMessage(message);
    entity.setInteractionJson(writeInteractionJson(interaction));
    entity.setState(OpenInconsistencyState.OPEN);
    entity.setCreatedAt(Instant.now());
    return openInconsistencyRepo.save(entity);
  }

  public boolean hasOpenForVsum(UUID vsumId) {
    return openInconsistencyRepo.findByVsumIdAndState(vsumId, OpenInconsistencyState.OPEN).isPresent();
  }

  public void markResolvedByTaskId(UUID taskId) {
    openInconsistencyRepo.findByTaskIdAndState(taskId, OpenInconsistencyState.OPEN).ifPresent(entity -> {
      entity.setState(OpenInconsistencyState.RESOLVED);
      entity.setResolvedAt(Instant.now());
      openInconsistencyRepo.save(entity);
    });
  }

  public void markFailedByTaskId(UUID taskId, String errorMessage) {
    openInconsistencyRepo.findByTaskIdAndState(taskId, OpenInconsistencyState.OPEN).ifPresent(entity -> {
      entity.setState(OpenInconsistencyState.FAILED);
      entity.setResolvedAt(Instant.now());
      if (errorMessage != null && !errorMessage.isBlank()) {
        entity.setMessage(errorMessage);
      }
      openInconsistencyRepo.save(entity);
    });
  }

  /**
   * Propagation tasks live in memory only. After a server restart, OPEN rows may reference
   * tasks that no longer exist and cannot be resolved from the hub.
   */
  private void reconcileStaleOpenInconsistencies() {
    for (OpenInconsistency entity : openInconsistencyRepo.findByStateOrderByCreatedAtDesc(OpenInconsistencyState.OPEN)) {
      if (!taskRegistry.taskExists(entity.getTaskId())) {
        markFailedByTaskId(
            entity.getTaskId(),
            "Propagation task lost after server restart. "
                + "Go to the editor, apply an update, and park a new inconsistency.");
      }
    }
  }

  public List<InconsistencyCommentResponse> listComments(UUID inconsistencyId) {
    getEntityOrThrow(inconsistencyId);
    return inconsistencyCommentRepo.findByInconsistencyIdOrderByCreatedAtAsc(inconsistencyId).stream()
        .map(this::toCommentResponse)
        .toList();
  }

  public InconsistencyCommentResponse addComment(UUID inconsistencyId, CreateInconsistencyCommentRequest request) {
    getEntityOrThrow(inconsistencyId);
    if (request == null || request.body() == null || request.body().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment body is required");
    }
    String author = request.author() != null && !request.author().isBlank()
        ? request.author().trim()
        : "anonymous";
    InconsistencyComment comment = new InconsistencyComment();
    comment.setInconsistencyId(inconsistencyId);
    comment.setAuthor(author);
    comment.setBody(request.body().trim());
    comment.setCreatedAt(Instant.now());
    return toCommentResponse(inconsistencyCommentRepo.save(comment));
  }

  public List<ViewUpdateSummaryResponse> listViewUpdates(UUID inconsistencyId) {
    OpenInconsistency entity = getEntityOrThrow(inconsistencyId);
    return viewUpdateRepo.findByVsumInfoIdOrderByTimestampDesc(entity.getVsumId()).stream()
        .map(this::toViewUpdateSummary)
        .toList();
  }

  public OpenInconsistencyResponse recordResolution(
      UUID inconsistencyId,
      RecordInconsistencyResolutionRequest request
  ) {
    OpenInconsistency entity = getEntityOrThrow(inconsistencyId);
    if (request == null || request.resolvedBy() == null || request.resolvedBy().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resolvedBy is required");
    }
    if (request.choice() == null || request.choice().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "choice is required");
    }

    entity.setResolvedBy(request.resolvedBy().trim());
    entity.setResolutionChoice(request.choice().trim());
    String comment = request.comment();
    entity.setResolutionComment(comment != null && !comment.isBlank() ? comment.trim() : null);
    if (entity.getResolvedAt() == null) {
      entity.setResolvedAt(Instant.now());
    }
    openInconsistencyRepo.save(entity);

    StringBuilder body = new StringBuilder();
    body.append("Resolved this inconsistency.\n");
    body.append("Resolver: ").append(entity.getResolvedBy()).append('\n');
    body.append("Selected choice: ").append(entity.getResolutionChoice());
    if (entity.getResolutionComment() != null && !entity.getResolutionComment().isBlank()) {
      body.append('\n').append("Resolution message: ").append(entity.getResolutionComment());
    }
    InconsistencyComment resolutionComment = new InconsistencyComment();
    resolutionComment.setInconsistencyId(entity.getId());
    resolutionComment.setAuthor(entity.getResolvedBy());
    resolutionComment.setBody(body.toString());
    resolutionComment.setCreatedAt(Instant.now());
    inconsistencyCommentRepo.save(resolutionComment);

    return toResponse(entity);
  }

  /**
   * Returns the model resource set for hub visualization.
   * For OPEN inconsistencies, prefers the committed resource set from propagation (includes user edits),
   * then the live parked view, then the snapshot captured at park time.
   */
  public InconsistencyModelSnapshotResponse getModelSnapshot(UUID inconsistencyId) {
    OpenInconsistency entity = getEntityOrThrow(inconsistencyId);

    if (entity.getState() == OpenInconsistencyState.OPEN && entity.getTaskId() != null) {
      AsyncTaskStatus task = taskRegistry.getTaskStatus(entity.getTaskId());
      if (task != null) {
        String committed = task.getCommittedResourceSet();
        if (committed != null && !committed.isBlank() && !"[]".equals(committed.trim())) {
          return toModelSnapshotFromEncoded(
              entity,
              committed,
              resolveViewTypeName(entity),
              Instant.now(),
              "Showing model committed when propagation started.");
        }
      }
    }

    if (entity.getState() == OpenInconsistencyState.OPEN && entity.getViewId() != null) {
      ViewWrapper viewWrapper = viewManager.getView(entity.getViewId());
      if (viewWrapper != null && !viewWrapper.view().isClosed()) {
        try {
          String encoded = viewService.serializeResourceSet(viewWrapper);
          if (encoded != null && !encoded.isBlank() && !"[]".equals(encoded.trim())) {
            return toModelSnapshotFromEncoded(
                entity,
                encoded,
                viewWrapper.view().getViewType().getName(),
                Instant.now(),
                "Showing current model while propagation is parked.");
          }
        } catch (RuntimeException e) {
          // Fall back to the snapshot captured at park time.
        }
      }
    }

    if (entity.getModelSnapshotEncodedResourceSet() != null
        && !entity.getModelSnapshotEncodedResourceSet().isBlank()) {
      return toModelSnapshotFromEncoded(
          entity,
          entity.getModelSnapshotEncodedResourceSet(),
          entity.getModelSnapshotViewTypeName(),
          entity.getCreatedAt(),
          "Showing model captured when this inconsistency was parked.");
    }

    ViewUpdate update = viewUpdateRepo.findFirstByVsumInfoIdOrderByTimestampDesc(entity.getVsumId());

    if (update != null && update.getEncodedResourceSet() != null && !update.getEncodedResourceSet().isBlank()) {
      return toModelSnapshot(entity, update, "Showing latest saved view snapshot.");
    }

    // Do not touch the live view while propagation is parked — the worker thread holds it.
    if (entity.getState() == OpenInconsistencyState.OPEN) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "No model snapshot for VSUM " + entity.getVsumId()
              + ". Park the inconsistency again so the model is captured at park time.");
    }

    ViewWrapper viewWrapper = entity.getViewId() != null ? viewManager.getView(entity.getViewId()) : null;
    if (viewWrapper != null && !viewWrapper.view().isClosed()) {
      try {
        String encoded = viewService.serializeResourceSet(viewWrapper);
        if (encoded != null && !encoded.isBlank() && !"[]".equals(encoded.trim())) {
          return toModelSnapshotFromEncoded(
              entity,
              encoded,
              viewWrapper.view().getViewType().getName(),
              Instant.now(),
              "Showing current open view (propagation paused at user interaction).");
        }
      } catch (RuntimeException e) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Could not read open view for visualization: " + e.getMessage());
      }
    }

    throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "No model snapshot for VSUM " + entity.getVsumId()
            + ". Open the editor, apply an update, then park the inconsistency again.");
  }

  private InconsistencyModelSnapshotResponse toModelSnapshot(
      OpenInconsistency entity,
      ViewUpdate update,
      String note
  ) {
    return toModelSnapshotFromEncoded(
        entity,
        update.getEncodedResourceSet(),
        update.getViewTypeName(),
        update.getTimestamp(),
        note + (update.getViewTypeName() != null ? " View type '" + update.getViewTypeName() + "'." : ""));
  }

  private InconsistencyModelSnapshotResponse toModelSnapshotFromEncoded(
      OpenInconsistency entity,
      String encodedResourceSet,
      String viewTypeName,
      Instant snapshotTimestamp,
      String note
  ) {
    if (encodedResourceSet == null || encodedResourceSet.isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Model snapshot is empty");
    }

    String enriched = modelSnapshotEnricher.enrich(encodedResourceSet, entity.getMessage());
    if (enriched == null || enriched.isBlank()) {
      enriched = encodedResourceSet;
    }

    String metamodelName = vsumInfoRepo.findById(entity.getVsumId())
        .map(VsumInfo::getMetaModelName)
        .orElse(null);

    return new InconsistencyModelSnapshotResponse(
        entity.getId(),
        entity.getVsumId(),
        entity.getVsumName(),
        metamodelName,
        viewTypeName,
        snapshotTimestamp,
        note + (viewTypeName != null ? " View type '" + viewTypeName + "'." : ""),
        enriched
    );
  }

  private String resolveViewTypeName(OpenInconsistency entity) {
    if (entity.getModelSnapshotViewTypeName() != null && !entity.getModelSnapshotViewTypeName().isBlank()) {
      return entity.getModelSnapshotViewTypeName();
    }
    if (entity.getViewId() == null) {
      return null;
    }
    ViewWrapper viewWrapper = viewManager.getView(entity.getViewId());
    if (viewWrapper == null || viewWrapper.view().isClosed()) {
      return null;
    }
    return viewWrapper.view().getViewType().getName();
  }

  private OpenInconsistency getEntityOrThrow(UUID id) {
    return openInconsistencyRepo.findById(id).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Inconsistency not found: " + id));
  }

  private OpenInconsistencyState parseState(String state) {
    try {
      return OpenInconsistencyState.valueOf(state.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Unknown state '" + state + "'; use OPEN, RESOLVED, FAILED, or ALL");
    }
  }

  private OpenInconsistencyResponse toResponse(OpenInconsistency entity) {
    String metamodelName = vsumInfoRepo.findById(entity.getVsumId())
        .map(VsumInfo::getMetaModelName)
        .orElse(null);
    return new OpenInconsistencyResponse(
        entity.getId(),
        entity.getVsumId(),
        entity.getTaskId(),
        entity.getViewId(),
        entity.getVsumName(),
        metamodelName,
        entity.getTitle(),
        entity.getMessage(),
        readInteractionJson(entity.getInteractionJson()),
        entity.getState(),
        entity.getCreatedAt(),
        entity.getResolvedAt(),
        entity.getResolvedBy(),
        entity.getResolutionChoice(),
        entity.getResolutionComment()
    );
  }

  private InconsistencyCommentResponse toCommentResponse(InconsistencyComment comment) {
    return new InconsistencyCommentResponse(
        comment.getId(),
        comment.getInconsistencyId(),
        comment.getAuthor(),
        comment.getBody(),
        comment.getCreatedAt()
    );
  }

  private ViewUpdateSummaryResponse toViewUpdateSummary(ViewUpdate update) {
    String encoded = update.getEncodedResourceSet();
    return new ViewUpdateSummaryResponse(
        update.getId(),
        update.getVsumInfo() != null ? update.getVsumInfo().getId() : null,
        update.getViewTypeName(),
        update.getSelectedObjectEClassNames() != null
            ? update.getSelectedObjectEClassNames()
            : List.of(),
        update.getTimestamp(),
        encoded != null ? encoded.length() : 0
    );
  }

  private String writeInteractionJson(Map<String, Object> interaction) {
    if (interaction == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(interaction);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store interaction JSON");
    }
  }

  private Map<String, Object> readInteractionJson(String json) {
    if (json == null || json.isBlank()) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<>() {
      });
    } catch (JsonProcessingException e) {
      return Collections.emptyMap();
    }
  }
}
