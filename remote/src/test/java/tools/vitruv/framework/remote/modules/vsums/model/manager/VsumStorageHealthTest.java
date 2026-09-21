package tools.vitruv.framework.remote.modules.vsums.model.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXParseException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class VsumStorageHealthTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsJsonWrittenIntoModelFile() throws Exception {
        Path model = tempDir.resolve("example.model");
        Files.writeString(model, "{\n  \"eClass\": \"model#//System\"\n}\n");

        assertThat(VsumStorageHealth.hasCorruptModelFiles(tempDir)).isTrue();
    }

    @Test
    void acceptsXmlModelFiles() throws Exception {
        Path model = tempDir.resolve("example.model");
        Files.writeString(model, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<model:System/>\n");

        assertThat(VsumStorageHealth.hasCorruptModelFiles(tempDir)).isFalse();
    }

    @Test
    void detectsIncompleteModelPair() throws Exception {
        Files.writeString(tempDir.resolve("example.model"), "<?xml version=\"1.0\"?><model:System/>");

        assertThat(VsumStorageHealth.hasIncompleteModelPair(tempDir)).isTrue();
        assertThat(VsumStorageHealth.repairIncompleteModelPair(tempDir)).isTrue();
        assertThat(Files.exists(tempDir.resolve("example.model"))).isFalse();
        assertThat(VsumStorageHealth.hasIncompleteModelPair(tempDir)).isFalse();
    }

    @Test
    void recognizesDanglingProxyErrors() {
        assertThat(VsumStorageHealth.isDanglingProxyFailure(
                new IllegalStateException("dangling object EntityImpl (eProxyURI: file:/x/example.model2#//@entities.0)")))
                .isTrue();
        assertThat(VsumStorageHealth.isDanglingProxyFailure(
                new IllegalStateException(
                        "new state 'file:/x/example.model2' should not contain proxies, but contains the following: "
                                + "EntityImpl (eProxyURI: //@entities.3)")))
                .isTrue();
    }

    @Test
    void recognizesPrologParseErrors() {
        RuntimeException error = new RuntimeException(
                "Failed to load resource",
                new SAXParseException("Content is not allowed in prolog.", null, null, 1, 1));

        assertThat(VsumStorageHealth.isXmlParseFailure(error)).isTrue();
        assertThat(VsumStorageHealth.isXmlParseFailure(new IllegalStateException("unrelated"))).isFalse();
    }
}
