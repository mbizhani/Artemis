package groovy.lang;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.devocative.artemis.ContextHandler;
import org.devocative.artemis.Parallel;
import org.devocative.artemis.log.ALog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Artemis {
	private static final List<String> CHARS = new ArrayList<>();
	private static final Random RANDOM = new Random();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Base64.Encoder B64_ENC = Base64.getEncoder();
	private static final Base64.Decoder B64_DEC = Base64.getDecoder();

	private static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	// ------------------------------

	static {
		for (Character c = '0'; c <= '9'; c++) {
			CHARS.add(c.toString());
		}
		for (Character c = 'a'; c <= 'z'; c++) {
			CHARS.add(c.toString());
		}
	}

	// ------------------------------

	public static void setDefaultCharset(Charset chs) {
		DEFAULT_CHARSET = chs;
	}

	// --- Random Data Generation

	public static String generate(int len) {
		return generate(len, CHARS);
	}

	@SafeVarargs
	public static String generate(int len, List<String>... alphaSets) {
		final List<String> alphabets = Arrays.stream(alphaSets)
			.flatMap(Collection::stream)
			.collect(Collectors.toList());

		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < len; i++) {
			builder.append(alphabets.get(RANDOM.nextInt(alphabets.size())));
		}
		return builder.toString();
	}

	public static int rand(int min, int max) {
		int res = RANDOM.nextInt(max);

		while (res < min) {
			res += RANDOM.nextInt(max);

			if (res > max) {
				res -= min;
			}
		}
		return res;
	}

	public static String uuid() {
		return UUID.randomUUID().toString();
	}

	public static int getThreadId() {
		final String name = Thread.currentThread().getName();
		final int idx = name.lastIndexOf(Parallel.THREAD_MIDIX);
		if (idx > 0) {
			final String threadId = name.substring(idx + Parallel.THREAD_MIDIX.length());
			return Integer.parseInt(threadId);
		}
		return 0;
	}

	// --- String Functions

	public static String format(Number number, String pattern) {
		final DecimalFormat df = new DecimalFormat(pattern);
		return df.format(number);
	}

	public static String format(Date date, String pattern) {
		final SimpleDateFormat df = new SimpleDateFormat(pattern);
		return df.format(date);
	}

	public static String jsonify(Object obj) {
		try {
			return MAPPER.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object objectify(String json) {
		try {
			return MAPPER.readValue(json, Object.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void log(String msg) {
		ALog.info("[Groovy] - " + msg);
	}

	// --- I/O

	public static HttpBuilder http() {
		return HttpBuilder.get();
	}

	public static String readFile(String name) {
		final InputStream in = ContextHandler.loadFile(name);

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			return reader.lines().collect(Collectors.joining("\n"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// --- Encryption/Decryption

	public static String encBase64(String str) {
		return encBase64(str, true);
	}

	public static String encBase64(String str, boolean withPadding) {
		return withPadding ?
			B64_ENC.encodeToString(str.getBytes(DEFAULT_CHARSET)) :
			B64_ENC.withoutPadding().encodeToString(str.getBytes(DEFAULT_CHARSET));
	}

	public static String decBase64(String str) {
		return new String(B64_DEC.decode(str.getBytes(DEFAULT_CHARSET)), DEFAULT_CHARSET);
	}

	public static KeyPairUnit genKeyPair() {
		return genKeyPair("RSA", 2048, "SHA256withRSA");
	}

	public static KeyPairUnit genKeyPair(String keyPairAlgorithm, int keySize, String signAlgorithm) {
		try {
			final KeyPairGenerator instance = KeyPairGenerator.getInstance(keyPairAlgorithm);
			instance.initialize(keySize);
			return new KeyPairUnit(instance.generateKeyPair(), signAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static X509Certificate loadCert(String certStr) {
		certStr = certStr
			.replaceAll("-----BEGIN CERTIFICATE-----", "")
			.replaceAll("-----END CERTIFICATE-----", "")
			.replaceAll("\n", "");

		try {
			return (X509Certificate) CertificateFactory
				.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(B64_DEC.decode(certStr)));
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}
	}

	public static KeyPairUnit loadKeyStore(String file, String password, String alias) {
		try (final InputStream in = ContextHandler.loadFile(file)) {
			final KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(in, password.toCharArray());

			final Key key = keyStore.getKey(alias, password.toCharArray());
			final Certificate certificate = keyStore.getCertificate(alias);

			if (key instanceof PrivateKey && certificate instanceof X509Certificate) {
				final PrivateKey privateKey = (PrivateKey) key;
				final X509Certificate x509Certificate = (X509Certificate) certificate;
				return new KeyPairUnit(privateKey, x509Certificate);
			} else {
				throw new RuntimeException("Unsupported KeyStore Entries: alias should be private and it must have a certificate");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
