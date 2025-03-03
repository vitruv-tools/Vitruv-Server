package utiil;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SSLUtils {
    public static PrivateKey extractPrivateKey(byte[] pemKey) throws GeneralSecurityException {
        try {
            // extract private key
            String pem = new String(pemKey);
            Pattern pattern = Pattern.compile("-----BEGIN PRIVATE KEY-----([A-Za-z0-9+/=\\s]+)-----END PRIVATE KEY-----", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(pem);

            if (!matcher.find()) {
                throw new GeneralSecurityException("Invalid PEM format");
            }

            String base64Key = matcher.group(1).replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);

            // convert to PKCS8
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

            // use correct algorithm (should be EC)
            try {
                return KeyFactory.getInstance("EC").generatePrivate(spec);
            } catch (Exception ignored) {
            }
            try {
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (Exception ignored) {
            }
            throw new GeneralSecurityException("Unsupported key format");
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to parse private key", e);
        }
    }
}
