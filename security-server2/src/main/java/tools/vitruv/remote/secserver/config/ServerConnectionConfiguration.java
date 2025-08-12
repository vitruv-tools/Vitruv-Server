package tools.vitruv.remote.secserver.config;

import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;

import tools.vitruv.framework.remote.common.AvailableHttpVersions;

public record ServerConnectionConfiguration(
    List<AvailableHttpVersions> httpVersions,
    String hostName,
    int port,
    String keyStorePath,
    KeyStore keyStore,
    String keyStorePassword,
    Path http3PemWorkDir) {
}
