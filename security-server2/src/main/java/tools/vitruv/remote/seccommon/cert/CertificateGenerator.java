package tools.vitruv.remote.seccommon.cert;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import tools.vitruv.remote.seccommon.SecurityProviderInitialization;

/**
 * This class provides generation methods for X.509 certificates
 * and related utility functions.
 */
public class CertificateGenerator {
    private static enum CertificateType {
        ROOT_CA_CERT,
        SERVER_AUTH_CA_CERT,
        SERVER_AUTH_CERT;
    }

    static {
        SecurityProviderInitialization.initializeSecurityProviders();
    }

    private static final String ROOT_CA_NAME = "Vitruvius Root-CA";
    private static final String SERVER_CA_NAME = "Vitruvius Server-CA";
    private static final String SERVER_LOCALHOST_NAME = "localhost";

    /**
     * Generates a full certificate chain for "localhost" (including private keys).
     * The chain consists of a self-signed root certificate, intermediate certificate
     * for signing server certificates, and the actual server certificate for "localhost".
     * The keys use RSA with 4096 bits.
     * 
     * @param keyStorePassword Password for the {@link KeyStore}, in which the certificates and private keys are stored.
     * @param keyStorePath Path, where the {@link KeyStore} with the certificates and private keys are stored.
     * @param trustStorePassword Password for the trust store, a {@link KeyStore}, in which the certificates only are stored.
     * @param trustStorePath Path, where the trust store with the certificates only is stored.
     * @throws NoSuchAlgorithmException If an algorithm used cannot be found.
     * @throws OperatorCreationException If something goes wrong.
     * @throws CertificateException If there is an issue with the certificates.
     * @throws IOException If there is an issue while reading or writing files.
     * @throws KeyStoreException If there is an issue with the {@link KeyStore}s.
     */
    public static void generateFullCertificateChainForLocalhost(
            String keyStorePassword,
            Path keyStorePath,
            String trustStorePassword,
            Path trustStorePath
            ) throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, IOException, KeyStoreException {
        generateFullCertificateChain(
            keyStorePassword,
            keyStorePath,
            trustStorePassword,
            trustStorePath,
            SERVER_LOCALHOST_NAME,
            new GeneralName[] {
                new GeneralName(GeneralName.iPAddress, "127.0.0.1"),
                new GeneralName(GeneralName.dNSName, "localhost")
            }
        );
    }

    /**
     * Generates a full certificate chain for a server (including private keys).
     * The chain consists of a self-signed root certificate, intermediate certificate
     * for signing server certificates, and the actual server certificate.
     * The keys use RSA with 4096 bits.
     * 
     * @param keyStorePassword Password for the {@link KeyStore}, in which the certificates and private keys are stored.
     * @param keyStorePath Path, where the {@link KeyStore} with the certificates and private keys are stored.
     * @param trustStorePassword Password for the trust store, a {@link KeyStore}, in which the certificates only are stored.
     * @param trustStorePath Path, where the trust store with the certificates only is stored.
     * @param serverCertCommonName The common name of the server, usually its host name or IP address.
     * @param serverAlternativeNames Alternative names for the server.
     * @throws NoSuchAlgorithmException If an algorithm used cannot be found.
     * @throws OperatorCreationException If something goes wrong.
     * @throws CertificateException If there is an issue with the certificates.
     * @throws IOException If there is an issue while reading or writing files.
     * @throws KeyStoreException If there is an issue with the {@link KeyStore}s.
     */
    public static void generateFullCertificateChain(
            String keyStorePassword,
            Path keyStorePath,
            String trustStorePassword,
            Path trustStorePath,
            String serverCertCommonName,
            GeneralName[] serverAlternativeNames
            ) throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, IOException, KeyStoreException {
        var keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(4096);
        
        var rootCaKeyPair = keyGenerator.generateKeyPair();
        var rootCaCertContainer = generateCertificateContainer(
            CertificateType.ROOT_CA_CERT,
            ROOT_CA_NAME,
            null,
            60 * 60 * 24 * 365 * 10, // 10 years
            rootCaKeyPair,
            rootCaKeyPair,
            null
        );
        var rootCaCert = convertToCertificate(rootCaCertContainer);

        var serverCaKeyPair = keyGenerator.generateKeyPair();
        var serverCaCertContainer = generateCertificateContainer(
            CertificateType.SERVER_AUTH_CA_CERT,
            SERVER_CA_NAME,
            null,
            60 * 60 * 24 * 365 * 5, // 5 years
            serverCaKeyPair,
            rootCaKeyPair,
            rootCaCertContainer.getSubject()
        );
        var serverCaCert = convertToCertificate(serverCaCertContainer);

        var serverAuthKeyPair = keyGenerator.generateKeyPair();
        var serverAuthCertContainer = generateCertificateContainer(
            CertificateType.SERVER_AUTH_CERT,
            serverCertCommonName,
            serverAlternativeNames,
            60 * 60 * 24 * 90, // 90 days
            serverAuthKeyPair,
            serverCaKeyPair,
            serverCaCertContainer.getSubject()
        );
        var serverAuthCert = convertToCertificate(serverAuthCertContainer);

        KeyStore keyStore = createEmptyKeyStore(keyStorePassword);
        keyStore.setKeyEntry(serverCertCommonName, serverAuthKeyPair.getPrivate(), keyStorePassword.toCharArray(), new Certificate[] { serverAuthCert });
        keyStore.setKeyEntry(SERVER_CA_NAME, serverCaKeyPair.getPrivate(), keyStorePassword.toCharArray(), new Certificate[] { serverCaCert });
        keyStore.setKeyEntry(ROOT_CA_NAME, rootCaKeyPair.getPrivate(), keyStorePassword.toCharArray(), new Certificate[] { rootCaCert });
        var writer = Files.newOutputStream(keyStorePath);
        keyStore.store(writer, keyStorePassword.toCharArray());
        writer.close();

        var trustStore = createEmptyKeyStore(trustStorePassword);
        trustStore.setCertificateEntry(serverCertCommonName, serverAuthCert);
        trustStore.setCertificateEntry(SERVER_CA_NAME, serverCaCert);
        trustStore.setCertificateEntry(ROOT_CA_NAME, rootCaCert);
        writer = Files.newOutputStream(trustStorePath);
        trustStore.store(writer, trustStorePassword.toCharArray());
        writer.close();
    }

    /**
     * Creates a new {@link KeyStore} and loads its content from a specified file.
     * 
     * @param keyStorePath Path to the {@link KeyStore} file.
     * @param password Password for the {@link KeyStore}.
     * @return the opened {@link KeyStore}.
     * @throws IOException If there is an issue while reading the file.
     * @throws KeyStoreException If the {@link KeyStore} cannot be created or loaded.
     * @throws NoSuchAlgorithmException If an algorithm cannot be found.
     * @throws CertificateException If there is an issue with the certificates.
     */
    public static KeyStore openKeyStore(Path keyStorePath, String password) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        var inputStream = Files.newInputStream(keyStorePath);
        keyStore.load(inputStream, password.toCharArray());
        inputStream.close();
        return keyStore;
    }

    /**
     * Creates a new and empty {@link KeyStore}.
     * 
     * @param password Password for the {@link KeyStore}.
     * @return The {@link KeyStore}.
     * @throws NoSuchAlgorithmException If an algorithm cannot be found.
     * @throws CertificateException If there is an issue with certificates.
     * @throws IOException If there is an issue with reading or writing files.
     * @throws KeyStoreException If there is an issue with the key stores.
     */
    public static KeyStore createEmptyKeyStore(String password) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, password.toCharArray());
        return keyStore;
    }

    private static X509CertificateHolder generateCertificateContainer(
            CertificateType certType,
            String commonName,
            GeneralName[] alternativeNames,
            long validSeconds,
            KeyPair signedKeys,
            KeyPair signingKeys,
            X500Name signingName
            ) throws CertIOException, OperatorCreationException, NoSuchAlgorithmException {
        var certBuilder = createCertificateBuilder(validSeconds, commonName, signedKeys, signingKeys, signingName);

        switch(certType) {
            case ROOT_CA_CERT:
            case SERVER_AUTH_CA_CERT:
                createCACertificateExtensions(certBuilder);
                break;
            case SERVER_AUTH_CERT:
                createServerAuthCertificateExtensions(certBuilder, alternativeNames);
                break;
            default:
                throw new UnsupportedOperationException("Certificate type not supported.");
        }

        return signCertificate(certBuilder, signingKeys.getPrivate());
    }

    private static X509v3CertificateBuilder createCertificateBuilder(
            long validSeconds,
            String commonName,
            KeyPair signedKeys,
            KeyPair signingKeys,
            X500Name signingName
            ) throws CertIOException, OperatorCreationException, NoSuchAlgorithmException {
        var now = Instant.now();
        var from = Date.from(now);
        var to = Date.from(now.plusSeconds(validSeconds));
        var serial = (long) (Math.floor(Math.random() * Long.MAX_VALUE));
        var hashedSerial = hash(serial);

        var name = createX500Name(commonName);

        var certBuilder = new JcaX509v3CertificateBuilder(signingName != null ? signingName : name, hashedSerial, from, to, name, signedKeys.getPublic());
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(signedKeys.getPublic()));
        if (signingName != null) {
            certBuilder.addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyIdentifier(signingKeys.getPublic()));
        }

        return certBuilder;
    }

    private static X500Name createX500Name(String commonName) {
        var nameBuilder = new X500NameBuilder();
        nameBuilder.addRDN(RFC4519Style.cn, commonName);
        nameBuilder.addRDN(RFC4519Style.c, "Germany");
        nameBuilder.addRDN(RFC4519Style.o, "SDQ, KASTEL, KIT");
        return nameBuilder.build();
    }

    private static BigInteger hash(long number) throws NoSuchAlgorithmException {
        long processedNumber = number;
        byte[] numberAsBytes = new byte[Long.BYTES];
        for (int byteIdx = 0; byteIdx < Long.BYTES; byteIdx++) {
            numberAsBytes[byteIdx] = (byte) (processedNumber & 0xFF);
            processedNumber >>= Byte.SIZE;
        }
        var md = MessageDigest.getInstance("SHA3-512");
        var hashValue = md.digest(numberAsBytes);
        return new BigInteger(hashValue);
    }

    private static AuthorityKeyIdentifier createAuthorityKeyIdentifier(PublicKey publicKey) throws OperatorCreationException {
        return createKeyIdUtil().createAuthorityKeyIdentifier(createPublicKeyInfo(publicKey));
    }

    private static SubjectKeyIdentifier createSubjectKeyIdentifier(PublicKey publicKey) throws OperatorCreationException {
        return createKeyIdUtil().createSubjectKeyIdentifier(createPublicKeyInfo(publicKey));
    }

    private static SubjectPublicKeyInfo createPublicKeyInfo(PublicKey publicKey) {
        return SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
    }

    private static X509ExtensionUtils createKeyIdUtil() throws OperatorCreationException {
        final var digCalc = new BcDigestCalculatorProvider().get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
        return new X509ExtensionUtils(digCalc);
    }

    private static void createCACertificateExtensions(X509v3CertificateBuilder certBuilder) throws CertIOException {
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
    }

    private static void createServerAuthCertificateExtensions(X509v3CertificateBuilder certBuilder, GeneralName[] altNames) throws CertIOException {
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        certBuilder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        certBuilder.addExtension(
            Extension.subjectAlternativeName,
            true,
            new GeneralNamesBuilder()
                .addNames(new GeneralNames(altNames))
                .build()
        );
    }

    private static X509CertificateHolder signCertificate(X509v3CertificateBuilder certBuilder, PrivateKey signingKey) throws OperatorCreationException {
        return certBuilder.build(
            new JcaContentSignerBuilder("SHA512WithRSA").build(signingKey)
        );
    }

    private static X509Certificate convertToCertificate(X509CertificateHolder certificateHolder) throws CertificateException {
        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }
}
