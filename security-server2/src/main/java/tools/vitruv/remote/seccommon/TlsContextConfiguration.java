package tools.vitruv.remote.seccommon;

import java.nio.file.Path;
import java.security.KeyStore;

public record TlsContextConfiguration(
    Path keyStorePath,
    KeyStore keyStore,
    String keyStorePassword,
    Path trustStorePath,
    KeyStore trustStore,
    String trustStorePassword,
    Path tempCertDir
) {}
