package tools.vitruv.remote.secclient;

import java.nio.file.Path;
import java.security.KeyStore;

public record SecurityClientConfiguration(
    String trustStorePath,
    KeyStore trustStore,
    String trustStorePassword,
    Path tempCertDir
) {}
