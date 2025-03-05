package tools.vitruv.framework.remote.common.apm;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class VitruvStepMeterRegistryTest {
  private VitruvStepMeterRegistry registry;
  private Path tempFile;
  private StepRegistryConfig mockConfig;
 
  @BeforeEach
  void setUp() throws IOException {
    tempFile = Files.createTempFile("metrics", ".log");

    mockConfig = mock(StepRegistryConfig.class);
    when(mockConfig.step()).thenReturn(Duration.of(1, ChronoUnit.MINUTES));
    when(mockConfig.enabled()).thenReturn(true);

    registry = new VitruvStepMeterRegistry(mockConfig, Clock.SYSTEM, tempFile);
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempFile);
  }
  
  @Test
  void testMetricsAreWrittenToFile() throws IOException {
    Timer timer = Timer.builder("test.timer").register(registry);
    timer.record(Duration.ofMillis(100));

    registry.publish();

    String fileContent = Files.readString(tempFile);
    assertTrue(fileContent.contains("test.timer"), "Metric ID should be present in the file");
  }

  @Test
  void testMultipleMetricsAreWrittenToFile() throws IOException {
    Timer timer1 = Timer.builder("test.timer1").register(registry);
    Timer timer2 = Timer.builder("test.timer2").register(registry);

    timer1.record(Duration.ofMillis(150));
    timer2.record(Duration.ofMillis(200));

    registry.publish();

    String fileContent = Files.readString(tempFile);
    assertTrue(fileContent.contains("test.timer1"), "Metric test.timer1 should be present in the file");
    assertTrue(fileContent.contains("test.timer2"), "Metric test.timer2 should be present in the file");
  }

  @Test
  void testFileRemainsEmptyIfNoMetricsAreRecorded() throws IOException {
    registry.publish();
    List<String> lines = Files.readAllLines(tempFile);
    assertTrue(lines.isEmpty(), "File should be empty if no metrics are recorded");
  }

  @Test
  void testFileWritingErrorHandling() throws IOException {
    // Make file read-only to simulate an error
    Files.writeString(tempFile, "Pre-existing content");
    tempFile.toFile().setReadOnly();

    registry.publish();

    // Restore write permission for cleanup
    tempFile.toFile().setWritable(true);

    String fileContent = Files.readString(tempFile);
    assertEquals("Pre-existing content", fileContent, "File content should remain unchanged due to error handling");
  }

  @Test
  void testTimersAreClearedAfterPublishing() throws IOException {
    // Register a timer and record a measurement
    Timer timer = registry.timer("test.timer");
    timer.record(Duration.ofMillis(100));

    // Ensure the timer has recorded values
    assertFalse(registry.getMeters().isEmpty(), "There should be at least one recorded metric before publishing.");

    // Call publish (which should clear the timer's stored measurements)
    registry.publish();

    // Check if the recorded timer still contains values
    boolean hasRemainingMetrics = registry.getMeters().stream()
        .anyMatch(meter -> meter.getId().getName().equals("test.timer"));

    assertTrue(hasRemainingMetrics, "Timer should still be registered.");
  }
}