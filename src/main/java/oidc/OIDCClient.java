package oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
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

        logger.info("Metadata Issuer: {}", issuer);
        logger.info("Provider Metadata discovered: {}", metadata);
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
            logger.error("Token request failed: " + response.toErrorResponse().getErrorObject().getDescription());
            throw new Exception("Token request failed: " + response.toErrorResponse().getErrorObject().getDescription());
        }

        AccessTokenResponse accessTokenResponse = response.toSuccessResponse();
        String idToken = accessTokenResponse.getCustomParameters().get("id_token").toString();
        logger.info("ID Token: {}", idToken);
        validateIDToken(idToken);

        return accessTokenResponse.getTokens();
    }

    public void validateIDToken(String idTokenString) throws Exception {
        SignedJWT idToken = SignedJWT.parse(idTokenString);
        // Create the JWT processor for validating signature & claims
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        // Load the JWK set from "https://oidc.scc.kit.edu/auth/realms/kit/protocol/openid-connect/certs"
        URL jwkSetURL = new URL(providerMetadata.getJWKSetURI().toString());
        JWKSet jwkSet = JWKSet.load(jwkSetURL);
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
        // Set up JWS key selector with RS256
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                JWSAlgorithm.RS256,
                jwkSource
        );
        jwtProcessor.setJWSKeySelector(keySelector);
        // verify idToken & extract JWTClaimsSet
        SecurityContext context = null; // Set a context if needed, otherwise null
        JWTClaimsSet claimsSet = jwtProcessor.process(idToken, context);
        // validate claims: issuer
        String issuer = claimsSet.getIssuer();
        if (!issuer.equals(providerMetadata.getIssuer().toString())) {
            logger.error("Invalid ID Token issuer: " + issuer);
            throw new Exception("Invalid ID Token issuer: " + issuer);
        }
        // validate claims: audience
        String audience = claimsSet.getAudience().get(0);
        if (!audience.equals(clientId)) {
            logger.error("Invalid ID Token audience: " + audience);
            throw new Exception("Invalid ID Token audience: " + audience);
        }
        String email = claimsSet.getClaim("email").toString();
        logger.info("Email of user: " + email);

        logger.info("ID Token is valid. Claims: " + claimsSet.toJSONObject());
    }
}
