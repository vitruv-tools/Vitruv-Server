package oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
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
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;

/**
 * Handles the OIDC communication flow with the FeLS identity provider.
 * Supports authorization requests, token exchange, validation, and token refresh using the refresh token.
 */
public class OIDCClient {

    private static final Logger logger = LoggerFactory.getLogger(OIDCClient.class);
    private final String clientId;
    private final String clientSecret;
    private final URI redirectUri;
    private OIDCProviderMetadata providerMetadata;
    /**
     * Static discovery URI for the FeLS OIDC provider. Not configurable, as only FeLS is supported.
     */
    private static final String DISCOVERY_URI = "https://fels.scc.kit.edu/oidc/realms/fels";

    /**
     * Initializes the OIDC client and fetches the provider metadata.
     *
     * @throws Exception if metadata discovery fails
     */
    public OIDCClient(String clientId, String clientSecret, String redirectUri) throws Exception {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = new URI(redirectUri);
        discoverProviderMetadata();

        logger.info("OIDC Client started.");
    }

    /**
     * Discovers the OIDC provider metadata using the discovery endpoint.
     *
     * @throws Exception if discovery fails
     */
    private void discoverProviderMetadata() throws Exception {
        Issuer issuer = new Issuer(new URI(DISCOVERY_URI));

        OIDCProviderConfigurationRequest request = new OIDCProviderConfigurationRequest(issuer);
        OIDCProviderMetadata metadata = OIDCProviderMetadata.parse(request.toHTTPRequest().send().getContentAsJSONObject());
        this.providerMetadata = metadata;

        logger.info("Metadata Issuer: {}", issuer);
        logger.debug("Provider Metadata discovered: {}", metadata);
    }

    public URI getAuthorizationRequestURI() {
        AuthorizationRequest request = new AuthorizationRequest.Builder(new ResponseType(ResponseType.Value.CODE), new ClientID(clientId))
                .endpointURI(providerMetadata.getAuthorizationEndpointURI())
                .redirectionURI(redirectUri)
                .scope(new Scope("openid", "profile", "email"))
                .build();
        return request.toURI();
    }

    /**
     * Exchanges an authorization code for access, ID, and refresh tokens.
     *
     * @param code Authorization code
     * @return AccessTokenResponse
     * @throws Exception if the token exchange fails
     */
    public AccessTokenResponse exchangeAuthorizationCode(String code) throws Exception {
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

        return response.toSuccessResponse();
    }

    /**
     * Validates the ID token's signature and claims.
     *
     * @param idTokenString ID Token as String
     * @throws Exception if the token is invalid
     */
    public void validateIDToken(String idTokenString) throws Exception {
        SignedJWT idToken = SignedJWT.parse(idTokenString);
        // create the JWT processor for validating signature & claims
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        // load the JWK set from "https://fels.scc.kit.edu/oidc/realms/fels/protocol/openid-connect/certs"
        URL jwkSetURL = new URL(providerMetadata.getJWKSetURI().toString());
        JWKSet jwkSet = JWKSet.load(jwkSetURL);
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
        // set up JWS key selector with RS256
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                JWSAlgorithm.RS256,
                jwkSource
        );
        jwtProcessor.setJWSKeySelector(keySelector);
        // verify idToken & extract JWTClaimsSet
        JWTClaimsSet claimsSet = jwtProcessor.process(idToken, null);
        logger.debug("Claims: {}", claimsSet.toJSONObject());

        validateClaims(claimsSet);

        logger.debug("Email of user: " + claimsSet.getClaim("email").toString());
        logger.info("ID Token is valid.");
    }

    /**
     * Validates the claims of the ID Token.
     *
     * @param claimsSet set of claims
     * @throws Exception if a claim is invalid
     */
    private void validateClaims(JWTClaimsSet claimsSet) throws Exception {
        // validate issuer
        String issuer = claimsSet.getIssuer();
        if (!issuer.equals(providerMetadata.getIssuer().toString())) {
            throw new Exception("Invalid ID Token issuer: " + issuer);
        }

        // validate audience
        String audience = claimsSet.getAudience().get(0);
        if (!audience.equals(clientId)) {
            throw new Exception("Invalid ID Token audience: " + audience);
        }
    }

    /**
     * Refreshes the access token using a refresh token.
     *
     * @param refreshToken Refresh Token
     * @return new Access and Refresh Token
     * @throws Exception if refresh fails
     */
    public AccessTokenResponse refreshAccessToken(String refreshToken) throws Exception {
        TokenRequest request = new TokenRequest(
                providerMetadata.getTokenEndpointURI(),
                new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecret)),
                new RefreshTokenGrant(new RefreshToken(refreshToken))
        );

        TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

        if (!response.indicatesSuccess()) {
            throw new Exception(response.toErrorResponse().getErrorObject().getDescription());
        }
        return response.toSuccessResponse();
    }

    /**
     * Checks whether an access token is valid and not expired.
     *
     * @param accessToken Access Token (JWT)
     * @return true if the token is valid, false otherwise
     */
    public boolean isAccessTokenValid(String accessToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();

            // check if token is expired
            if (expiration == null || expiration.before(new Date())) {
                logger.error("Access Token expired");
                return false;
            }

            // check if signature is valid
            return validateSignature(signedJWT);
        } catch (ParseException e) {
            logger.error("Error parsing Access Token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifies the signature of a signed Access Token.
     *
     * @param signedJWT Token to be verified
     * @return true if the signature is valid, false otherwise
     */
    private boolean validateSignature(SignedJWT signedJWT) {
        try {
            // fetch JWKS URI and find matching key
            URL jwkSetURL = new URL(providerMetadata.getJWKSetURI().toString());
            JWKSet jwkSet = JWKSet.load(jwkSetURL);
            JWK key = jwkSet.getKeyByKeyId(signedJWT.getHeader().getKeyID());

            if (key == null) {
                logger.error("No matching key found for kid={}.", signedJWT.getHeader().getKeyID());
                return false;
            }

            // verify signature
            RSAPublicKey publicKey = (RSAPublicKey) key.toRSAKey().toPublicKey();
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            boolean isValid = signedJWT.verify(verifier);

            if (!isValid) {
                logger.error("Invalid JWT signature.");
            } else {
                logger.debug("Valid JWT signature.");
            }
            return isValid;

        } catch (ParseException | IOException | JOSEException e) {
            logger.error("Signature validation failed: {}", e.getMessage());
            return false;
        }
    }
}
