package org.devocative.artemis.test;

import io.javalin.Javalin;
import org.devocative.artemis.ArtemisExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

	Javalin app;
	ArtemisExecutor executor;

	@BeforeEach
	void init() {
		executor = new ArtemisExecutor();

		if ("local".equals(executor.getProfile())) {
			app = Javalin
				.create()
				.start(8080);

			app
				.post("/registrations", ctx -> {
					final Map<String, String> data = ctx.bodyAsClass(Map.class);
					final String cell = data.get("cell");

					log("Register - cell=[{}], name=[{}]", cell, data.get("name"));

					ctx.json(asMap(
						pair("smsCode", Math.abs(cell.hashCode())),
						pair("token", UUID.randomUUID().toString())
					));
				})
				.put("/registrations", ctx -> {
					final Map<String, String> data = ctx.bodyAsClass(Map.class);
					log("Verify - smsCode=[{}], authHeader=[{}]", data.get("smsCode"), ctx.header("Authorization"));

					ctx
						.json(asMap(
							pair("userId", UUID.randomUUID().toString())
						))
						.status(201);
				})
				.put("/users/:id", ctx -> {
					final Map<String, String> data = ctx.bodyAsClass(Map.class);
					log("UpdateProfile - id=[{}] data={} authHeader=[{}]",
						ctx.pathParam("id"), data, ctx.header("Authorization"));
				});
		}
	}

	@Test
	public void main() throws Exception {
		executor.execute();
	}

	@AfterEach
	void tearDown() {
		if (app != null) {
			app.stop();
		}
	}

	// ------------------------------

	private static Map<String, Object> asMap(Pair... pairs) {
		return Arrays.stream(pairs).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	private static void log(String str, Object... vars) {
		log.info("--- TEST --- | " + str, vars);
	}
}
