package org.devocative.artemis.test;

import groovy.lang.Artemis;
import groovy.lang.HttpBuilder;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static groovy.lang.Artemis.http;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestArtemis {

	@Test
	public void test_http() {
		final Javalin app = TestArtemisExecutor.startJavalin(8080);

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

		app.close();
	}

	// ------------------------------

	private String url(String uri, String... params) {
		final String url = String.format("http://localhost:8080%s", uri);
		return String.format(url, params);
	}
}
