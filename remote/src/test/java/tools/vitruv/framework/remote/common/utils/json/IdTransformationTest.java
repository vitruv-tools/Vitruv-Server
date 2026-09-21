package tools.vitruv.framework.remote.common.utils.json;

import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IdTransformationTest {

    @TempDir
    Path storage;

    @Test
    void toGlobal_relativeKnownModel_resolvesUnderStorage() {
        IdTransformation transformation = new IdTransformation(storage);

        URI global = transformation.toGlobal(URI.createURI("/example.model2"));

        assertThat(global.isFile()).isTrue();
        assertThat(global.toString()).doesNotContain("file%3A");
        assertThat(global.toFileString().replace('\\', '/'))
                .isEqualTo(storage.resolve("example.model2").toAbsolutePath().normalize().toString().replace('\\', '/'));
    }

    @Test
    void toGlobal_absoluteFileUriForKnownModel_doesNotDoubleWrapFileScheme() {
        IdTransformation transformation = new IdTransformation(storage);
        URI misplaced = URI.createFileURI(
                storage.getRoot().resolve("VitruviusServer").resolve("example.model2").toString().replace('\\', '/'));

        URI global = transformation.toGlobal(misplaced);

        assertThat(global.isFile()).isTrue();
        assertThat(global.toString()).doesNotContain("file%3A");
        assertThat(global.toString()).startsWith("file:");
        // Must not nest file: inside a filesystem path under the server cwd
        assertThat(global.toFileString()).doesNotContain("file:");
        assertThat(global.toFileString().replace('\\', '/'))
                .isEqualTo(storage.resolve("example.model2").toAbsolutePath().normalize().toString().replace('\\', '/'));
    }

    @Test
    void toLocal_and_toGlobal_roundTrip() {
        IdTransformation transformation = new IdTransformation(storage);
        URI original = URI.createFileURI(storage.resolve("example.model").toString().replace('\\', '/'));

        URI local = transformation.toLocal(original);
        URI global = transformation.toGlobal(local);

        assertThat(local.toString()).isEqualTo("/example.model");
        assertThat(global.toFileString().replace('\\', '/'))
                .isEqualTo(original.toFileString().replace('\\', '/'));
    }
}
