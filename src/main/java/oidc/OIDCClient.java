package oidc;

import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;

public class OIDCClient {

    private static final Logger logger = LoggerFactory.getLogger(OIDCClient.class);

    private final String clientId;
    private final String clientSecret;
    private final URI redirectUri;
    private OIDCProviderMetadata providerMetadata;

    public OIDCClient(String clientId, String clientSecret, String redirectUri) throws Exception {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = new URI(redirectUri);
        discoverProviderMetadata();

        logger.info("OIDC-Client started.");
    }

    private void discoverProviderMetadata() throws Exception {
        URI discoveryUri = new URI("https://oidc.scc.kit.edu/auth/realms/kit");
        Issuer issuer = new Issuer(discoveryUri);
        OIDCProviderConfigurationRequest request = new OIDCProviderConfigurationRequest(issuer);
        OIDCProviderMetadata metadata = OIDCProviderMetadata.parse(request.toHTTPRequest().send().getContentAsJSONObject());
        this.providerMetadata = metadata;
        logger.info("Provider Metadata discovered: {}", metadata.getIssuer());
    }

    public URI getAuthorizationRequestURI() {
        AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType(ResponseType.Value.CODE), new ClientID(clientId))
                .endpointURI(providerMetadata.getAuthorizationEndpointURI())
                .redirectionURI(redirectUri)
                .scope(new Scope("openid", "profile", "email"))
                .build();
        return request.toURI();
    }

    public Tokens exchangeAuthorizationCode(String code) throws Exception {
        AuthorizationCode authorizationCode = new AuthorizationCode(code);
        TokenRequest request = new TokenRequest(
                providerMetadata.getTokenEndpointURI(),
                new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecret)),
                new AuthorizationCodeGrant(authorizationCode, redirectUri));

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            throw new Exception("Token request failed: " + response.toErrorResponse().getErrorObject().getDescription());
        }

        return response.toSuccessResponse().getTokens();
    }

    public void validateIDToken(String idTokenString) throws Exception {
        SignedJWT idToken = SignedJWT.parse(idTokenString);
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

        RemoteJWKSet<SecurityContext> jwkSet = new RemoteJWKSet<>(new URL(providerMetadata.getJWKSetURI().toString()));
        IDTokenValidator validator = new IDTokenValidator(providerMetadata.getIssuer(), new ClientID(clientId));

        validator.validate(idToken, null);
        logger.info("ID Token is valid.");
    }
}
