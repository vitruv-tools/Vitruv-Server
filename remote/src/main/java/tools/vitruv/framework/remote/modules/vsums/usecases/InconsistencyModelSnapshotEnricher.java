package tools.vitruv.framework.remote.modules.vsums.usecases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ensures hub model snapshots include the entity named in a parked inconsistency message
 * (e.g. a newly inserted Entity awaiting component-type selection).
 */
@Component
@RequiredArgsConstructor
public class InconsistencyModelSnapshotEnricher {

  private static final Pattern NAMED_ENTITY_PATTERN = Pattern.compile(
      "(?:entity|component)\\s+['\"]([^'\"]+)['\"]",
      Pattern.CASE_INSENSITIVE
  );

  private final ObjectMapper objectMapper;

  public String enrich(String encodedResourceSet, String message) {
    if (encodedResourceSet == null || encodedResourceSet.isBlank() || message == null || message.isBlank()) {
      return encodedResourceSet;
    }

    Matcher matcher = NAMED_ENTITY_PATTERN.matcher(message);
    if (!matcher.find()) {
      return encodedResourceSet;
    }

    String entityName = matcher.group(1).trim();
    if (entityName.isEmpty() || "null".equalsIgnoreCase(entityName)) {
      return encodedResourceSet;
    }

    try {
      JsonNode resources = objectMapper.readTree(encodedResourceSet);
      if (!resources.isArray()) {
        return encodedResourceSet;
      }

      boolean changed = false;
      for (JsonNode resource : resources) {
        JsonNode content = resource.get("content");
        if (content == null || !content.isObject()) {
          continue;
        }

        String eClass = content.path("eClass").asText("");
        if (!eClass.contains("//Root")) {
          continue;
        }

        JsonNode entities = content.get("entities");
        if (entities != null && containsEntityNamed(entities, entityName)) {
          return encodedResourceSet;
        }

        ObjectNode entityNode = objectMapper.createObjectNode();
        entityNode.put("name", entityName);

        ObjectNode contentObject = (ObjectNode) content;
        if (entities == null || entities.isNull()) {
          contentObject.set("entities", objectMapper.createArrayNode().add(entityNode));
        } else if (entities.isArray()) {
          ((ArrayNode) entities).add(entityNode);
        } else {
          continue;
        }

        changed = true;
        break;
      }

      return changed ? objectMapper.writeValueAsString(resources) : encodedResourceSet;
    } catch (Exception e) {
      return encodedResourceSet;
    }
  }

  private boolean containsEntityNamed(JsonNode entities, String entityName) {
    if (!entities.isArray()) {
      return false;
    }

    for (JsonNode entity : entities) {
      String name = entity.path("name").asText("");
      if (entityName.equalsIgnoreCase(name)) {
        return true;
      }
    }
    return false;
  }
}
