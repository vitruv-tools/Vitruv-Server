package tools.vitruv.framework.remote.common.json;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface JsonDeserializationHelper {
    <T> T deserialize(JsonNode node, Class<T> clazz) throws IOException;

    <T> List<T> deserializeArrayOf(String json, Class<T> clazz) throws JsonProcessingException;

    Resource deserializeResource(String json, String id, ResourceSet set) throws JsonProcessingException;
}
