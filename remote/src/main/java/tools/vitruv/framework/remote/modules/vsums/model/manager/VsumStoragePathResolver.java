package tools.vitruv.framework.remote.modules.vsums.model.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Resolves on-disk VSUM folders under {@code vsum-storage}.
 * <p>
 * Prefer the stable {@code <metamodel>-<uuid>} directory so restarts reuse the same models.
 * Legacy folders that still have a random suffix are reused only when the stable folder is missing.
 * Corrupt JSON-in-XMI folders are quarantined instead of being loaded.
 */
public final class VsumStoragePathResolver {

    static final String CORRUPT_DIR_NAME = ".corrupt";
    private static final DateTimeFormatter CORRUPT_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private VsumStoragePathResolver() {
    }

    public static String directoryPrefix(String metaModelName, UUID id) {
        return metaModelName.replaceAll("\\s", "") + "-" + id;
    }

    public static Path resolve(Path storageRoot, String metaModelName, UUID id) throws IOException {
        String prefix = directoryPrefix(metaModelName, id);
        Files.createDirectories(storageRoot);

        Path exact = storageRoot.resolve(prefix);
        if (Files.isDirectory(exact)) {
            return exact;
        }

        try (Stream<Path> stream = Files.list(storageRoot)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> isLegacyFolder(path, prefix))
                    .max(Comparator.comparingLong(VsumStoragePathResolver::lastModifiedMillis))
                    .orElse(exact);
        }
    }

    /**
     * Resolves a reusable folder, moving aside storage whose model files are JSON instead of XMI.
     */
    public static Path resolveReplacingCorrupt(Path storageRoot, String metaModelName, UUID id) throws IOException {
        Path resolved = resolve(storageRoot, metaModelName, id);
        if (VsumStorageHealth.hasCorruptModelFiles(resolved)) {
            quarantine(resolved);
            return storageRoot.resolve(directoryPrefix(metaModelName, id));
        }
        return resolved;
    }

    public static Path quarantine(Path storagePath) throws IOException {
        if (storagePath == null || !Files.exists(storagePath)) {
            return storagePath;
        }

        Path storageRoot = storagePath.getParent();
        Path corruptRoot = storageRoot.resolve(CORRUPT_DIR_NAME);
        Files.createDirectories(corruptRoot);

        Path destination = corruptRoot.resolve(storagePath.getFileName() + "-" + CORRUPT_STAMP.format(Instant.now()));
        try {
            Files.move(storagePath, destination);
        } catch (IOException moveFailed) {
            copyDirectory(storagePath, destination);
            deleteRecursively(storagePath);
        }
        return destination;
    }

    private static boolean isLegacyFolder(Path path, String prefix) {
        String name = path.getFileName().toString();
        return name.startsWith(prefix)
                && !name.equals(CORRUPT_DIR_NAME)
                && !name.contains(CORRUPT_DIR_NAME);
    }

    private static long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static void copyDirectory(Path source, Path destination) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            walk.forEach(path -> {
                Path relative = source.relativize(path);
                Path target = relative.toString().isEmpty() ? destination : destination.resolve(relative);
                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(entry -> {
                try {
                    Files.deleteIfExists(entry);
                } catch (IOException ignored) {
                    // best-effort cleanup after quarantine copy
                }
            });
        }
    }
}
