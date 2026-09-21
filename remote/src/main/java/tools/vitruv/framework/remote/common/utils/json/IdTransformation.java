package tools.vitruv.framework.remote.common.utils.json;

import org.eclipse.emf.common.util.URI;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.atomic.root.RootEChange;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Transforms Vitruv resource URIs between client-local form ({@code /example.model}) and absolute
 * vsum-storage paths.
 *
 * <p>Uses the VSUM storage folder passed at construction time. Legacy deployments may still send
 * commits with URIs under {@code VitruviusServer/} after {@code persistProjectRelative} misplaced
 * model files; {@link #toLocal} / {@link #toGlobal} normalize known filenames
 * ({@code example.model}, {@code example.model2} — used by SystemRoot and AmaltheaAscet).
 *
 * <p>{@link #toGlobal} resolves known model files via {@link Path#resolve(String)} and
 * {@link URI#createFileURI(String)} on a filesystem path only. Never pass an existing
 * {@code file:} URI string into {@code createFileURI}, or Windows paths like
 * {@code .../file%3A/E:/.../example.model2} result.
 */
public class IdTransformation {
    private static final Set<String> KNOWN_MODEL_FILES = Set.of("example.model", "example.model2");

    private final Path vsumPath;
    private final URI root;

    /**
     * Creates a new IdTransformation.
     *
     * @param vsumPath the path to the .vsum file of the project
     */
    public IdTransformation(Path vsumPath) {
        // Use only this VSUM storage folder. ProjectMarker.getProjectRootFolder() can walk up to
        // the VitruviusServer working directory when persistProjectRelative wrote models there.
        this.vsumPath = vsumPath.toAbsolutePath().normalize();
        this.root = URI.createFileURI(this.vsumPath.toString().replace('\\', '/'));
    }

    /**
     * Transforms the given global (absolute path) ID to a local ID (relative path).
     *
     * @param global The ID to transform.
     * @return The local ID.
     */
    public URI toLocal(URI global) {
        if (global == null
                || global.toString().contains("cache")
                || global.toString().equals(JsonFieldName.TEMP_VALUE)
                || !global.isFile()) {
            return global;
        }

        String globalStr = normalizePath(global.toString());
        String rootStr = normalizePath(root.toString());
        if (globalStr.startsWith(rootStr)) {
            return URI.createURI(globalStr.substring(rootStr.length()));
        }

        String modelFile = global.lastSegment();
        if (KNOWN_MODEL_FILES.contains(modelFile)) {
            return URI.createURI("/" + modelFile);
        }

        return global;
    }

    /**
     * Transforms the given local ID (relative path) to a global ID (absolute path).
     *
     * @param local The ID to transform.
     * @return The global ID.
     */
    public URI toGlobal(URI local) {
        if (local == null
                || local.toString().contains("cache")
                || local.toString().equals(JsonFieldName.TEMP_VALUE)) {
            return local;
        }

        String modelFile = local.lastSegment();
        if (modelFile != null && KNOWN_MODEL_FILES.contains(modelFile)) {
            // Always resolve via Path — never URI.createFileURI(root.toString() + ...), which
            // double-wraps the file: scheme and yields paths like .../file%3A/E:/.../example.model2.
            return fileUri(vsumPath.resolve(modelFile));
        }

        if (!local.isRelative()) {
            return local;
        }

        String relative = local.toString();
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.isEmpty()) {
            return root;
        }
        return fileUri(vsumPath.resolve(relative));
    }

    private static URI fileUri(Path path) {
        return URI.createFileURI(path.toAbsolutePath().normalize().toString().replace('\\', '/'));
    }

    private static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    /**
     * Transforms all root change URIs in the given list of changes to global IDs.
     *
     * @param eChanges the list of changes
     */
    public void allToGlobal(List<? extends EChange<HierarchicalId>> eChanges) {
        for (var eChange : eChanges) {
            if (eChange instanceof RootEChange<?> change) {
                change.setUri(toGlobal(URI.createURI(change.getUri())).toString());
            }
        }
    }

    /**
     * Transforms all root change URIs in the given list of changes to local IDs.
     *
     * @param eChanges the list of changes
     */
    public void allToLocal(List<? extends EChange<HierarchicalId>> eChanges) {
        for (var eChange : eChanges) {
            if (eChange instanceof RootEChange<?> change) {
                change.setUri(toLocal(URI.createURI(change.getUri())).toString());
            }
        }
    }
}
