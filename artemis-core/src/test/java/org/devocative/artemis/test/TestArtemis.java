package org.devocative.artemis.test;

import groovy.lang.Artemis;
import groovy.lang.HttpBuilder;
import groovy.lang.KeyPairUnit;
import org.devocative.artemis.ContextHandler;
import org.devocative.artemis.cfg.Config;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static groovy.lang.Artemis.http;
import static org.junit.jupiter.api.Assertions.*;

public class TestArtemis {
	private String url;

	@Test
	public void test_rand() {
		for (int i = 15; i < 1000; i += 5) {
			final int rand = Artemis.rand(10, i);
			assertTrue(rand >= 10);
			assertTrue(rand <= i);
		}

		{
			final int rand = Artemis.rand(0, 3);
			assertTrue(rand >= 0);
			assertTrue(rand <= 3);
		}
	}

	@Test
	public void test_http() {
		TestArtemisExecutor.startJavalin(url -> {
			this.url = url;

			final String rand = Artemis.generate(3, Arrays.asList("1", "8"));
			final String cell = "09" + Artemis.generate(9, Arrays.asList("1", "8"));

			{
				final HttpBuilder.Rs register = http()
					.post(url("/registrations?_p=%s&p1=%s&list=l1&list=l2", rand, rand))
					.body(String.format("{\"name\": \"Foo Bar\", \"cell\": \"%s\"}", cell))
					.header("randHead", "1" + rand)
					.send();

				System.out.println("registerRs = " + register);
				assertEquals(200, register.getCode());
			}

			final Object smsCode;
			{
				final HttpBuilder.Rs fetchCode = http().get(url("/registrations/%s?p1=%s", cell, rand))
					.header("randHead", "3" + rand)
					.send();

				System.out.println("fetchCodeRs = " + fetchCode);
				smsCode = ((Map) fetchCode.getBodyAsObject()).get("smsCode");
				assertEquals(200, fetchCode.getCode());
				assertNotNull(smsCode);
			}

			{
				final HttpBuilder.Rs verify = http().put(url("/registrations"))
					.body(String.format("{\"smsCode\": \"%s\", \"password\": \"saccxc3es\"}", smsCode))
					.header("randHead", "2" + rand)
					.send();

				System.out.println("verifyRs = " + verify);
				assertEquals(201, verify.getCode());
			}
		});
	}

	@Test
	public void test_enc() {
		final Config config = new Config();
		config.init();
		ContextHandler.init(config);

		final KeyPairUnit keyPair = Artemis.genKeyPair();

		final PrivateKey privateKey = keyPair.getPrivateKey();
		assertEquals("RSA", privateKey.getAlgorithm());
		assertEquals("PKCS#8", privateKey.getFormat());

		final PublicKey publicKey = keyPair.getPublicKey();
		assertEquals("RSA", publicKey.getAlgorithm());
		assertEquals("X.509", publicKey.getFormat());

		{
			final String raw = UUID.randomUUID().toString();
			final String sign = keyPair.sign(raw);
			assertTrue(keyPair.verifySign(raw, sign));
		}

		{
			final String signAlg = "SHA256withRSA";
			final String raw = UUID.randomUUID().toString();
			final String signed = KeyPairUnit.sign(signAlg, privateKey, raw);
			assertTrue(KeyPairUnit.verifySign(signAlg, publicKey, raw, signed));
		}

		{
			final String signAlg = "SHA1withRSA";
			final String raw = UUID.randomUUID().toString();
			final String signed = KeyPairUnit.sign(signAlg, privateKey, raw);
			assertTrue(KeyPairUnit.verifySign(signAlg, publicKey, raw, signed));
		}

		try {
			final X509Certificate publicPem = Artemis.loadCert(Artemis.readFile("my-cert.pem"));
			System.out.println("'my-cert.pem' - sigAlgName() = " + publicPem.getSigAlgName());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		{
			final String raw = UUID.randomUUID().toString();
			final KeyPairUnit keyStore = Artemis.loadKeyStore("my.pfx", "mypass", "myalias");
			final String signed = keyStore.sign(raw);
			assertTrue(keyStore.verifySign(raw, signed));
		}
	}

	// ------------------------------

	private String url(String uri, String... params) {
		final String url = String.format("%s%s", this.url, uri);
		return String.format(url, params);
	}
}
