package tools.vitruv.remote.secserver.config;

import java.util.List;

import tools.vitruv.framework.remote.common.AvailableHttpVersions;
import tools.vitruv.remote.seccommon.TlsContextConfiguration;

public record ServerConnectionConfiguration(
    List<AvailableHttpVersions> httpVersions,
    String hostName,
    int port,
    TlsContextConfiguration tlsConfig) {
}
