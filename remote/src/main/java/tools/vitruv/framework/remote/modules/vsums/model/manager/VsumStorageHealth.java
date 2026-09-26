package tools.vitruv.framework.remote.modules.vsums.model.manager;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Detects VSUM storage that Vitruv cannot load as XMI, typically JSON written into {@code example.model},
 * or an incomplete {@code example.model} / {@code example.model2} pair (SystemRoot / AmaltheaAscet).
 */
public final class VsumStorageHealth {

  private static final String MODEL_FILE = "example.model";
  private static final String MODEL2_FILE = "example.model2";

  private VsumStorageHealth() {
  }

  /**
   * SystemRoot and AmaltheaAscet need both model files. If only one exists, correspondences point at
   * a missing resource and the UI shows dangling EMF proxies.
   */
  public static boolean hasIncompleteModelPair(Path storagePath) {
    if (storagePath == null || !Files.isDirectory(storagePath)) {
      return false;
    }
    boolean hasModel = Files.exists(storagePath.resolve(MODEL_FILE));
    boolean hasModel2 = Files.exists(storagePath.resolve(MODEL2_FILE));
    return hasModel != hasModel2;
  }

  /**
   * Wipes an incomplete model pair and Vitruv metadata so the provider can re-seed on next load.
   *
   * @return {@code true} when repair was applied
   */
  public static boolean repairIncompleteModelPair(Path storagePath) {
    if (!hasIncompleteModelPair(storagePath)) {
      return false;
    }

    deleteQuietly(storagePath.resolve("vsum").resolve("correspondences.correspondence"));
    deleteQuietly(storagePath.resolve("vsum").resolve("models.models"));
    deleteRecursivelyQuietly(storagePath.resolve("consistencymetadata"));
    deleteQuietly(storagePath.resolve(MODEL_FILE));
    deleteQuietly(storagePath.resolve(MODEL2_FILE));
    return true;
  }

  public static boolean hasCorruptModelFiles(Path storagePath) {
    if (storagePath == null || !Files.isDirectory(storagePath)) {
      return false;
    }

    try (Stream<Path> stream = Files.walk(storagePath, 3)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(VsumStorageHealth::isModelFile)
          .anyMatch(VsumStorageHealth::looksLikeJson);
    } catch (IOException e) {
      return false;
    }
  }

  public static boolean isDanglingProxyFailure(Throwable error) {
    Throwable current = error;
    while (current != null) {
      String message = current.getMessage();
      if (message != null) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("dangling object")
            || normalized.contains("eproxyuri")
            || normalized.contains("should not contain proxies")) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }

  public static boolean isXmlParseFailure(Throwable error) {
    Throwable current = error;
    while (current != null) {
      String message = current.getMessage();
      if (message != null) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("content is not allowed in prolog")
            || normalized.contains("premature end of file")
            || normalized.contains("saxparseexception")) {
          return true;
        }
      }
      if (current.getClass().getName().contains("SAXParseException")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  static boolean isModelFile(Path path) {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
    return name.endsWith(".model") || name.endsWith(".model2") || name.endsWith(".xmi");
  }

  static boolean looksLikeJson(Path path) {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      int value;
      while ((value = reader.read()) != -1) {
        if (!Character.isWhitespace(value)) {
          return value == '{' || value == '[';
        }
      }
      return false;
    } catch (IOException e) {
      return false;
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // best-effort repair before load
    }
  }

  private static void deleteRecursivelyQuietly(Path dir) {
    if (!Files.isDirectory(dir)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(dir)) {
      walk.sorted((a, b) -> b.compareTo(a)).forEach(path -> deleteQuietly(path));
    } catch (IOException ignored) {
      // best-effort
    }
  }
}
