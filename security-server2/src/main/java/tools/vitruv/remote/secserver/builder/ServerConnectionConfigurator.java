package tools.vitruv.remote.secserver.builder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import tools.vitruv.framework.remote.common.AvailableHttpVersions;
import tools.vitruv.remote.seccommon.TlsContextConfiguration;
import tools.vitruv.remote.secserver.config.ServerConnectionConfiguration;

public class ServerConnectionConfigurator {
    private List<AvailableHttpVersions> httpVersions = new ArrayList<>();
    private String hostName;
    private int port;
    private String keyStorePath;
    private KeyStore keyStore;
    private String keyStorePassword;
    private Path pemWorkDir;

    ServerConnectionConfigurator() {}

    public ServerConnectionConfigurator withHttp11() {
        this.httpVersions.add(AvailableHttpVersions.HTTP_1_1);
        return this;
    }

    public ServerConnectionConfigurator withHttp11(boolean enableHttp11) {
        if (enableHttp11) {
            return withHttp11();
        }
        return this;
    }

    public ServerConnectionConfigurator withHttp2() {
        this.httpVersions.add(AvailableHttpVersions.HTTP_2);
        return this;
    }

    public ServerConnectionConfigurator withHttp2(boolean enableHttp2) {
        if (enableHttp2) {
            return withHttp2();
        }
        return this;
    }

    public ServerConnectionConfigurator withExperimentalHttp3(Path pemWorkDir) {
        this.httpVersions.add(AvailableHttpVersions.HTTP_3);
        this.pemWorkDir = pemWorkDir;
        return this;
    }

    public ServerConnectionConfigurator withExperimentalHttp3(boolean enableHttp3, Path pemWorkDir) {
        if (enableHttp3) {
            return withExperimentalHttp3(pemWorkDir);
        }
        return this;
    }

    public ServerConnectionConfigurator forHostNameOrIp(String hostNameOrIp) {
        this.hostName = hostNameOrIp;
        return this;
    }

    public ServerConnectionConfigurator onPort(int port) {
        this.port = port;
        return this;
    }

    public ServerConnectionConfigurator usingKeyStore(KeyStore keyStore, String keyStorePassword) {
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public ServerConnectionConfigurator usingKeyStorePath(String keyStorePath, String keyStorePassword) {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public DirectConnectionModeConfigurator operateInDirectConnectionMode() {
        return new DirectConnectionModeConfigurator(getConnectionConfiguration());
    }

    public ReverseProxyModeConfigurator operateInReverseProxyMode() {
        return new ReverseProxyModeConfigurator(getConnectionConfiguration());
    }

    public ProxyModeConfigurator operateInProxyMode() {
        return new ProxyModeConfigurator(getConnectionConfiguration());
    }

    private ServerConnectionConfiguration getConnectionConfiguration() {
        return new ServerConnectionConfiguration(
            this.httpVersions,
            this.hostName,
            this.port,
            new TlsContextConfiguration(
                Paths.get(this.keyStorePath),
                this.keyStore,
                this.keyStorePassword,
                null,
                null,
                null,
                this.pemWorkDir
            )
        );
    }
}
