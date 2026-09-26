package tools.vitruv.framework.remote.modules.vsums.model.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.vitruv.framework.remote.config.VsumProperties;
import tools.vitruv.framework.remote.vsumprovider.VsumProvider;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Service responsible for dynamically loading and managing implementations of {@code VsumProvider}.
 * The implementations are loaded from specified directories and their associated JAR dependencies
 * at runtime using the {@code URLClassLoader}.
 * <p>
 * Providers share one classloader so EMF packages with the same NS URI
 * (e.g. SystemRootVsum and InteractionDemoVsum) are registered once.
 * Lib JARs with the same filename are deduplicated; {@code SystemRootVsum} is loaded first so its
 * consistency routines (Epic 1 validators / link-speed dialog) are not shadowed by InteractionDemo.
 */
@Service
@RequiredArgsConstructor
public class VsumProviderLoader {
  private final VsumProperties vsumProperties;

  private URLClassLoader sharedClassLoader;

  /**
   * Loads all available {@code VsumProvider} implementations from the configured provider directories.
   *
   * @return a list of loaded {@code VsumProvider} instances. If no providers are found or the
   * provider directories are not accessible, an empty list is returned.
   * @throws Exception if an unexpected error occurs during the loading process.
   */
  public synchronized List<VsumProvider> loadAll() throws Exception {
    Path allProvidersDir = Path.of(vsumProperties.vsumProvidersDir());
    String jarName = vsumProperties.vsumProviderJar();
    String libDirName = vsumProperties.vsumProviderLibDirName();

    if (!Files.exists(allProvidersDir) || !Files.isDirectory(allProvidersDir)) {
      return List.of();
    }

    List<URL> urls = new ArrayList<>();
    Set<String> seenLibFileNames = new HashSet<>();
    boolean anyProvider = false;

    try (Stream<Path> dirs = Files.list(allProvidersDir)) {
      List<Path> providerDirs = dirs
          .filter(Files::isDirectory)
          .sorted(systemRootFirst())
          .toList();

      for (Path vsumDir : providerDirs) {
        Path providerJar = vsumDir.resolve(jarName);
        if (!Files.exists(providerJar)) {
          continue;
        }
        anyProvider = true;
        // Each provider jar must be present for ServiceLoader (same filename, different path).
        urls.add(providerJar.toUri().toURL());

        Path libDir = vsumDir.resolve(libDirName);
        if (Files.exists(libDir) && Files.isDirectory(libDir)) {
          try (Stream<Path> libs = Files.list(libDir)) {
            libs.filter(p -> p.toString().endsWith(".jar"))
                .sorted()
                .forEach(p -> {
                  String fileName = p.getFileName().toString();
                  // Same artifact from InteractionDemo vs SystemRoot: keep first (SystemRoot).
                  if (!seenLibFileNames.add(fileName)) {
                    return;
                  }
                  try {
                    urls.add(p.toUri().toURL());
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                });
          }
        }
      }
    }

    if (!anyProvider) {
      return List.of();
    }

    if (sharedClassLoader == null) {
      sharedClassLoader = new URLClassLoader(
          urls.toArray(new URL[0]),
          VsumProvider.class.getClassLoader());
    }

    List<VsumProvider> vsumProviders = new ArrayList<>();
    ServiceLoader<VsumProvider> loader = ServiceLoader.load(VsumProvider.class, sharedClassLoader);
    for (VsumProvider provider : loader) {
      vsumProviders.add(provider);
    }
    return vsumProviders;
  }

  /**
   * Prefer SystemRootVsum so its consistency JAR wins filename deduplication.
   */
  private static Comparator<Path> systemRootFirst() {
    return Comparator
        .comparing((Path p) -> {
          String name = p.getFileName().toString();
          if (name.equals("SystemRootVsum")) {
            return 0;
          }
          if (name.equals("InteractionDemoVsum")) {
            return 2;
          }
          return 1;
        })
        .thenComparing(p -> p.getFileName().toString());
  }
}
