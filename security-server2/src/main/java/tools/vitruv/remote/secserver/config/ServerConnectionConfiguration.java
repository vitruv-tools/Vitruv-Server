package tools.vitruv.remote.secserver.config;

import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;

public record ServerConnectionConfiguration(
    List<AvailableHttpVersions> httpVersions,
    String hostName,
    int port,
    String keyStorePath,
    KeyStore keyStore,
    String keyStorePassword,
    Path http3PemWorkDir) {
}
