package tools.vitruv.framework.remote.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.vitruv.change.utils.ProjectMarker;

class IdTransformationTest {
  @TempDir Path folder;

  @Test
  void uriInProjectBecomesRelativeToProjectRoot() throws Exception {
    ProjectMarker.markAsProjectRootFolder(folder);
    Path vsum = Files.createDirectories(folder.resolve("vsum"));

    URI local =
        new IdTransformation(vsum).toLocal(URI.createFileURI(folder.resolve("a.model").toString()));

    assertEquals("/a.model", local.toString());
  }

  @Test
  void outermostProjectFolderIsRoot() throws Exception {
    ProjectMarker.markAsProjectRootFolder(folder);
    Path inner = Files.createDirectories(folder.resolve("inner"));
    ProjectMarker.markAsProjectRootFolder(inner);

    URI local =
        new IdTransformation(inner).toLocal(URI.createFileURI(inner.resolve("a.model").toString()));

    assertEquals("/inner/a.model", local.toString());
  }

  @Test
  void pathWithoutProjectFolderIsRoot() {
    URI local =
        new IdTransformation(folder)
            .toLocal(URI.createFileURI(folder.resolve("a.model").toString()));

    assertEquals("/a.model", local.toString());
  }

  @Test
  void localUriBecomesGlobal() throws Exception {
    ProjectMarker.markAsProjectRootFolder(folder);
    IdTransformation transformation = new IdTransformation(folder);
    URI global = URI.createFileURI(folder.resolve("a.model").toString());

    assertEquals(global, transformation.toGlobal(transformation.toLocal(global)));
  }
}
