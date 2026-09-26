package tools.vitruv.framework.remote.modules.vsums.model.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import tools.vitruv.framework.remote.config.VsumProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for loading and processing Ecore model files associated with specific metamodels.
 * It assumes a directory structure wherein Ecore model files are organized within subdirectories
 * under a base providers directory. The service reads these files, converts them from XML to JSON,
 * and aggregates them into a JSON array.
 * <p>
 * The processed JSON data provides a representation of the models that can be consumed by
 * other parts of the application.
 * <p>
 * This class handles tasks such as file discovery, XML parsing, and JSON serialization.
 * It uses Jackson's ObjectMapper and XmlMapper for transformations.
 */
@Service
@RequiredArgsConstructor
public class EcoreModelLoader {

  private final VsumProperties vsumProperties;

  private final XmlMapper xmlMapper = new XmlMapper();
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Loads and processes Ecore model files for a given metamodel. It searches for files with a ".ecore"
   * extension in a specific directory structure based on the provided metamodel name, parses each file
   * into JSON, and aggregates them into a JSON array string.
   *
   * @param metamodelName the name of the metamodel whose Ecore models should be loaded
   * @return a JSON array string containing the processed Ecore models
   * @throws IllegalStateException if the directory for the Ecore models does not exist or is not a directory
   * @throws RuntimeException      if an error occurs while listing files, reading files, or processing their content
   */
  public String loadEcoreModels(String metamodelName) {
    Path ecoreModelsDir = Path.of(vsumProperties.vsumProvidersDir())
        .resolve(metamodelName)
        .resolve("ecore-models");

    if (!Files.isDirectory(ecoreModelsDir)) {
      throw new IllegalStateException(
          "Ecore models directory does not exist or is not a directory for metamodel '" +
              metamodelName + "': " + ecoreModelsDir
      );
    }

    try (val files = Files.list(ecoreModelsDir)) {
      List<Path> ecoreFiles = files
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".ecore"))
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .toList();

      val ecoreModelObjects = ecoreFiles.stream()
          .map(this::parseEcoreAsJson)
          .collect(Collectors.joining(","));

      return "[" + ecoreModelObjects + "]";

    } catch (IOException e) {
      throw new RuntimeException(
          "Failed to list ecore-model files for metamodel '" + metamodelName +
              "' in directory: " + ecoreModelsDir,
          e
      );
    }
  }

  private String parseEcoreAsJson(Path ecoreFile) {
    JsonNode xmlAsTree = readXmlTree(ecoreFile);
    return writeJsonString(xmlAsTree, ecoreFile);
  }

  private JsonNode readXmlTree(Path ecoreFile) {
    try {
      return xmlMapper.readTree(ecoreFile.toFile());
    } catch (IOException e) {
      throw new RuntimeException(
          "Failed to parse ecore-model file as XML: " + ecoreFile,
          e
      );
    }
  }

  private String writeJsonString(JsonNode node, Path sourceFile) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          "Failed to serialize parsed ecore-model to JSON for file: " + sourceFile,
          e
      );
    }
  }
}
