package groovy.lang;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class KeyPairUnit {
	private static final Base64.Encoder B64_ENC = Base64.getEncoder();
	private static final Base64.Decoder B64_DEC = Base64.getDecoder();

	// ------------------------------

	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	private final String signAlgorithm;

	private X509Certificate certificate;

	// ------------------------------

	public KeyPairUnit(KeyPair keyPair, String signAlgorithm) {
		this(keyPair.getPrivate(), keyPair.getPublic(), signAlgorithm);
	}

	public KeyPairUnit(PrivateKey privateKey, X509Certificate certificate) {
		this(privateKey, certificate.getPublicKey(), certificate.getSigAlgName());
		this.certificate = certificate;
	}

	// Main Constructor
	public KeyPairUnit(PrivateKey privateKey, PublicKey publicKey, String signAlgorithm) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.signAlgorithm = signAlgorithm;
	}

	// ------------------------------

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

	public String getEncodedPrivateKey() {
		return B64_ENC.encodeToString(privateKey.getEncoded());
	}

	public String getEncodedPublicKey() {
		return B64_ENC.encodeToString(publicKey.getEncoded());
	}

	public String getEncodedCertificate() {
		try {
			return B64_ENC.encodeToString(certificate.getEncoded());
		} catch (CertificateEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	// ---------------

	public String sign(String signAlgorithm, String raw) {
		return sign(signAlgorithm, privateKey, raw);
	}

	public String sign(String raw) {
		return sign(signAlgorithm, privateKey, raw);
	}

	public boolean verifySign(String signAlgorithm, String raw, String signed) {
		return verifySign(signAlgorithm, publicKey, raw, signed);
	}

	public boolean verifySign(String raw, String signed) {
		return verifySign(signAlgorithm, publicKey, raw, signed);
	}

	// ------------------------------

	public static String sign(String signAlgorithm, PrivateKey privateKey, String raw) {
		try {
			final Signature signature = Signature.getInstance(signAlgorithm);
			signature.initSign(privateKey);
			signature.update(raw.getBytes(StandardCharsets.UTF_8));
			return B64_ENC.encodeToString(signature.sign());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean verifySign(X509Certificate cert, String raw, String signed) {
		return verifySign(cert.getSigAlgName(), cert.getPublicKey(), raw, signed);
	}

	public static boolean verifySign(String signAlgorithm, PublicKey publicKey, String raw, String signed) {
		try {
			final Signature signature = Signature.getInstance(signAlgorithm);
			signature.initVerify(publicKey);
			signature.update(raw.getBytes(StandardCharsets.UTF_8));
			return signature.verify(B64_DEC.decode(signed));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
