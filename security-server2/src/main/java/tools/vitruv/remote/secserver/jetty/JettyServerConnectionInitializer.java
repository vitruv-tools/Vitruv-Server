package tools.vitruv.remote.secserver.jetty;

import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import tools.vitruv.framework.remote.common.AvailableHttpVersions;
import tools.vitruv.remote.seccommon.AddressBinderUtil;
import tools.vitruv.remote.secserver.config.ServerConnectionConfiguration;

import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory;
import org.eclipse.jetty.quic.server.QuicServerConnector;
import org.eclipse.jetty.quic.server.ServerQuicConfiguration;

/**
 * This class initializes the connectors for a Jetty server so that it is able to process incoming requests.
 */
public class JettyServerConnectionInitializer {
    private JettyServerConnectionInitializer() {}

    public static void initializeConnectors(Server server, ServerConnectionConfiguration config) {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setHttpCompliance(HttpCompliance.RFC7230);
        httpConfig.setSendServerVersion(false);

        var tlsCustomizer = new SecureRequestCustomizer();
        tlsCustomizer.setSniHostCheck(false);
        httpConfig.addCustomizer(tlsCustomizer);
        
        boolean useHttp11 = config.httpVersions().contains(AvailableHttpVersions.HTTP_1_1);
        boolean useHttp2 = config.httpVersions().contains(AvailableHttpVersions.HTTP_2);

        List<ConnectionFactory> connectionFactories = new ArrayList<>();
        String nextProtocolAfterTls = "";

        if (useHttp11) {
            HttpConnectionFactory https11 = new HttpConnectionFactory(httpConfig);
            connectionFactories.add(https11);
            nextProtocolAfterTls = https11.getProtocol();
        }

        if (useHttp2) {
            HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);
            connectionFactories.add(0, h2);
            
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            alpn.setDefaultProtocol(connectionFactories.get(connectionFactories.size() - 1).getProtocol());
            connectionFactories.add(0, alpn);
            nextProtocolAfterTls = alpn.getProtocol();
        }
        
        SslContextFactory.Server tlsContext = new SslContextFactory.Server();
        tlsContext.setProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        if (config.tlsConfig().keyStorePath() != null) {
            tlsContext.setKeyStorePath(config.tlsConfig().keyStorePath().toString());
        } else if (config.tlsConfig().keyStore() != null) {
            tlsContext.setKeyStore(config.tlsConfig().keyStore());
        }
        tlsContext.setKeyStorePassword(config.tlsConfig().keyStorePassword());
        tlsContext.setCertAlias(config.hostName());
        tlsContext.addExcludeProtocols("TLSv1.2");
        SslConnectionFactory tls = new SslConnectionFactory(tlsContext, nextProtocolAfterTls);
        connectionFactories.add(0, tls);

        ServerConnector connector = new ServerConnector(server, connectionFactories.toArray(new ConnectionFactory[connectionFactories.size()]));
        connector.setHost(AddressBinderUtil.getAddressForBinding(config.hostName()));
        connector.setPort(config.port());
        server.addConnector(connector);

        if (config.httpVersions().contains(AvailableHttpVersions.HTTP_3)) {
            httpConfig.addCustomizer(new Http3AlternativeServiceCustomizer(config.port()));
            
            ServerQuicConfiguration quicConfig = new ServerQuicConfiguration(tlsContext, config.tlsConfig().tempCertDir());
            QuicServerConnector h3 = new QuicServerConnector(server, quicConfig, new HTTP3ServerConnectionFactory(quicConfig));
            h3.setHost(AddressBinderUtil.getAddressForBinding(config.hostName()));
            h3.setPort(config.port());

            server.addConnector(h3);
        }
    }
}
