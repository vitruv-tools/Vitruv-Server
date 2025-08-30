package tools.vitruv.remote.secserver.config;

import java.io.IOException;

/**
 * A manager for the configuration, which loads everything from environment variables.
 */
public class EnvConfigManager extends ConfigManager {
    @Override
    public void initialize() throws IOException {
    }

    @Override
    public int getVitruvServerPort() {
        return Integer.parseInt(System.getenv(KEY_VITRUVIUS_SERVER_PORT));
    }

    @Override
    public int getHttpsServerPort() {
        return Integer.parseInt(System.getenv(KEY_HTTPS_SERVER_PORT));
    }

    @Override
    public String getDomainProtocol() {
        return System.getenv(KEY_DOMAIN_PROTOCOL);
    }

    @Override
    public String getDomainName() {
        return System.getenv(KEY_DOMAIN_NAME);
    }

    @Override
    public String getCertChainPath() {
        return System.getenv(KEY_CERT_CHAIN_PATH);
    }

    @Override
    public String getCertKeyPath() {
        return System.getenv(KEY_CERT_KEY_PATH);
    }

    @Override
    public String getOidcClientId() {
        return System.getenv(KEY_OIDC_CLIENT_ID);
    }

    @Override
    public String getOidcClientSecret() {
        return System.getenv(KEY_OIDC_CLIENT_SECRET);
    }

    @Override
    public String getTlsPassword() {
        return System.getenv(KEY_TLS_PASSWORD);
    }

    @Override
    public String getOIDCDiscoveryUri() {
        return System.getenv(KEY_OIDC_DISCOVERY_URI);
    }
}
