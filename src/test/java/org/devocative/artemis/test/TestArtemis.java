package org.devocative.artemis.test;

import io.javalin.Javalin;
import org.devocative.artemis.ArtemisExecutor;
import org.devocative.artemis.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.devocative.artemis.test.Pair.pair;

public class TestArtemis {
	private static final Logger log = LoggerFactory.getLogger(TestArtemis.class);

	@Test
	public void test_defaultConfig() {
		final Javalin app = Javalin
			.create()
			.start(8080);

		configure(app);

		ArtemisExecutor.run();
	}

	@Test
	public void test_customBaseUrl() {
		final Javalin app = Javalin
			.create()
			.start(7777);

		configure(app);

		ArtemisExecutor.run(new Config().setBaseUrl("http://localhost:7777"));
	}

	@Test
	public void test_customProfile() {
		final Javalin app = Javalin
			.create()
			.start(8888);

		configure(app);

		// 'baseUrl' is set inside artemis.groovy:init()
		ArtemisExecutor.run(new Config().setProfile("test"));
	}

	// ------------------------------

	private void configure(Javalin app) {
		app
			.post("/registrations", ctx -> {
				final Map<String, String> data = ctx.bodyAsClass(Map.class);
				log("Register (Sending SMS) - {}", data);
			})
			.get("/registrations/:cell", ctx -> {
				final String cell = ctx.pathParam("cell");
				log("Query (Sent SMS) - {}", cell);

				ctx.json(asMap(
					pair("smsCode", Math.abs(cell.hashCode()))
				));
			})
			.put("/registrations", ctx -> {
				final Map<String, String> data = ctx.bodyAsClass(Map.class);
				log("Verify - {}", data);

				ctx.status(201)
					.json(asMap(
						pair("token", UUID.randomUUID().toString()),
						pair("userId", UUID.randomUUID().toString())
					));
			})
			.put("/users/:id", ctx -> {
				final Map<String, String> data = ctx.bodyAsClass(Map.class);
				log("UpdateProfile - id=[{}] data={} authHeader=[{}]",
					ctx.pathParam("id"), data, ctx.header("Authorization"));
			});

		app
			.get("/login/:cell", ctx -> {
				final String cell = ctx.pathParam("cell");
				log("GetLoginCode - cell=[{}]", cell);

				ctx.json(asMap(
					pair("smsCode", Math.abs(cell.hashCode()))
				));
			})
			.post("/login", ctx -> {
				final Map<String, String> data = ctx.bodyAsClass(Map.class);
				log("Login - {}", data);

				ctx.json(asMap(
					pair("token", UUID.randomUUID().toString())
				));
			});
	}

	private static Map<String, Object> asMap(Pair... pairs) {
		return Arrays.stream(pairs).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	private static void log(String str, Object... vars) {
		log.info("--- TEST --- | " + str, vars);
	}
}
