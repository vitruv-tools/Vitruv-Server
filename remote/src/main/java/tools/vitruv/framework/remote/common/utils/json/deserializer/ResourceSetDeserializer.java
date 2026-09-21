package tools.vitruv.framework.remote.common.utils.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import tools.vitruv.framework.remote.common.utils.ResourceUtils;
import tools.vitruv.framework.remote.common.utils.json.IdTransformation;
import tools.vitruv.framework.remote.common.utils.json.JsonFieldName;
import tools.vitruv.framework.remote.common.utils.json.JsonMapper;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.IOException;
import java.util.Map;

/**
 * A deserializer for {@link ResourceSet}.
 */
public class ResourceSetDeserializer extends JsonDeserializer<ResourceSet> {
    private final IdTransformation transformation;
    private final JsonMapper mapper;

    /**
     * Creates a new ResourceSetDeserializer.
     *
     * @param mapper         The json mapper to be used.
     * @param transformation the id transformation to be used
     */
    public ResourceSetDeserializer(JsonMapper mapper, IdTransformation transformation) {
        this.transformation = transformation;
        this.mapper = mapper;
    }

    @Override
    public ResourceSet deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        var rootNode = (ArrayNode) parser.getCodec().readTree(parser);

        var resourceSet = ResourceUtils.createJsonResourceSet();
        for (var e : rootNode) {
            var resource =
                    mapper.deserializeResource(
                            e.get(JsonFieldName.CONTENT).toString(),
                            transformation.toGlobal(URI.createURI(e.get(JsonFieldName.URI).asText())).toString(),
                            resourceSet);
            if (!resource.getURI().toString().equals(JsonFieldName.TEMP_VALUE)) {
                resource.save(Map.of());
            }
        }
        return resourceSet;
    }
}
