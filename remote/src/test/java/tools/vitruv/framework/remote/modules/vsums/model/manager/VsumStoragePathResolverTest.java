package tools.vitruv.framework.remote.modules.vsums.model.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class VsumStoragePathResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void reusesExistingFolderWithRandomSuffix() throws Exception {
        UUID id = UUID.fromString("5b363731-fce3-4403-9d47-ac78d2f4ca4e");
        Path existing = tempDir.resolve("SystemRootVsum-" + id + "TwjYU1yN");
        Files.createDirectories(existing);

        Path resolved = VsumStoragePathResolver.resolve(tempDir, "SystemRootVsum", id);

        assertThat(resolved).isEqualTo(existing);
    }

    @Test
    void prefersStableFolderOverLegacyRandomSuffix() throws Exception {
        UUID id = UUID.fromString("5b363731-fce3-4403-9d47-ac78d2f4ca4e");
        Path stable = tempDir.resolve("SystemRootVsum-" + id);
        Path legacy = tempDir.resolve("SystemRootVsum-" + id + "TwjYU1yN");
        Files.createDirectories(stable);
        Files.createDirectories(legacy);

        Path resolved = VsumStoragePathResolver.resolve(tempDir, "SystemRootVsum", id);

        assertThat(resolved).isEqualTo(stable);
    }

    @Test
    void createsDeterministicFolderWhenNoneExists() throws Exception {
        UUID id = UUID.fromString("3280d503-b6a0-416d-8c43-565a659dc69b");

        Path resolved = VsumStoragePathResolver.resolve(tempDir, "InteractionDemoVsum", id);

        assertThat(resolved.getFileName().toString())
                .isEqualTo("InteractionDemoVsum-" + id);
        assertThat(Files.exists(resolved)).isFalse();
    }

    @Test
    void quarantinesJsonModelFolderAndReturnsFreshPath() throws Exception {
        UUID id = UUID.fromString("5b363731-fce3-4403-9d47-ac78d2f4ca4e");
        Path existing = tempDir.resolve("SystemRootVsum-" + id);
        Files.createDirectories(existing);
        Files.writeString(existing.resolve("example.model"), "{ \"eClass\": \"model#//System\" }\n");

        Path resolved = VsumStoragePathResolver.resolveReplacingCorrupt(tempDir, "SystemRootVsum", id);

        assertThat(resolved).isEqualTo(tempDir.resolve("SystemRootVsum-" + id));
        assertThat(Files.exists(existing)).isFalse();
        try (Stream<Path> quarantined = Files.list(tempDir.resolve(VsumStoragePathResolver.CORRUPT_DIR_NAME))) {
            assertThat(quarantined.map(path -> path.getFileName().toString()))
                    .anyMatch(name -> name.startsWith("SystemRootVsum-" + id));
        }
    }
}
