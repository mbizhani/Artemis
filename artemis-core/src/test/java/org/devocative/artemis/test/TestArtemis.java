package org.devocative.artemis.test;

import io.javalin.Javalin;
import io.javalin.core.validation.Validator;
import org.devocative.artemis.ArtemisExecutor;
import org.devocative.artemis.cfg.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.devocative.artemis.test.Pair.pair;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestArtemis {
	private static final Logger log = LoggerFactory.getLogger(TestArtemis.class);

	@Test
	public void test_defaultConfig() {
		final Javalin app = Javalin
			.create()
			.start(8080);

		configure(app);

		ArtemisExecutor.run(new Config()
			.addVar("backEnd", "http://localhost:8080")
			.setParallel(1));

		app.stop();
	}

	@Test
	public void test_setBaseUrlViaConfig() {
		final String baseUrl = "http://localhost:7777";

		final Javalin app = Javalin
			.create()
			.start(7777);

		configure(app);

		ArtemisExecutor.run(new Config()
			.setDevMode(true)
			.setConsoleLog(false)
			.setBaseUrl(baseUrl)
			.setBaseDir("src/test/resources")
			.addVar("backEnd", baseUrl));

		app.stop();
	}

	@Test
	public void test_setBaseUrlViaSysProp() {
		final String baseUrl = "http://localhost:8888";

		final Javalin app = Javalin
			.create()
			.start(8888);

		configure(app);

		System.setProperty("artemis.base.url", baseUrl);
		ArtemisExecutor.run(new Config()
			.setParallel(8)
			.addVar("backEnd", baseUrl));
		System.clearProperty("artemis.base.url");

		app.stop();
	}

	// ------------------------------

	private void configure(Javalin app) {
		app.before(context ->
			context.headerAsClass("randHead", Integer.class)
				.check(val -> {
					System.out.println("val = " + val);
					switch (context.method()) {
						case "POST":
							return val > 1000 && val < 2000;
						case "PUT":
							return val > 2000 && val < 3000;
						case "GET":
							return val > 3000 && val < 4000;
					}
					return false;
				}, "Invalid 'randHead' Header")
				.get()
		);

		app
			.post("/registrations", ctx -> {
				final Validator<String> _p = ctx
					.queryParamAsClass("_p", String.class)
					.check(s -> s.length() == 3, "Invalid '_p' param length");
				final Validator<Integer> p1 = ctx.queryParamAsClass("p1", Integer.class)
					.check(i -> i > 0, "Invalid param 'p1' value, should be greater than 0");

				final Map<String, String> data = ctx.bodyAsClass(Map.class);
				log("Register (Sending SMS) - {}, _p={}, p1={}", data, _p.get(), p1.get());

				ctx.contentType("");
			})
			.get("/registrations/{cell}", ctx -> {
				final String cell = ctx.pathParam("cell");
				log("Query (Sent SMS) - {}", cell);

				ctx.queryParamAsClass("p1", Integer.class)
					.check(i -> i > 0, "Invalid param 'p1', should be greater than 0")
					.get();

				ctx
					.cookie("Cookie1", "11")
					.cookie("Cookie2", "22")
					.json(asMap(
						pair("smsCode", Math.abs(cell.hashCode()))
					));
			})
			.put("/registrations", ctx -> {
				assertEquals("11", ctx.cookie("Cookie1"));
				assertEquals("22", ctx.cookie("Cookie2"));

				final Map<String, String> data = ctx.bodyAsClass(Map.class);
				log("Verify - {}", data);

				ctx.status(201)
					.cookie("Cookie1", "111")
					.json(asMap(
						pair("token", UUID.randomUUID().toString()),
						pair("userId", UUID.randomUUID().toString()),
						pair("nullProp", null)
					));
			})
			.put("/users/{id}", ctx -> {
				assertEquals("111", ctx.cookie("Cookie1"));
				assertEquals("22", ctx.cookie("Cookie2"));

				final Validator<String> city = ctx.formParamAsClass("city", String.class)
					.check(s -> s.startsWith("artemis"), "Invalid param 'city', should starts with 'artemis'");
				final Validator<String> email = ctx.formParamAsClass("email", String.class)
					.check(s -> s.matches("\\w+@(\\w+\\.)+\\w+"), "Invalid email format");

				log("UpdateProfile - id=[{}], authHeader=[{}], city={}, email={}",
					ctx.pathParam("id"), ctx.header("Authorization"), city.get(), email.get());

				ctx.cookie("Cookie1", "", 0);
			});

		app
			.get("/login/{cell}", ctx -> {
				assertEquals("22", ctx.cookie("Cookie2"));

				final String cell = ctx.pathParam("cell");
				log("GetLoginCode - cell=[{}]", cell);

				ctx.json(asMap(
					pair("smsCode", Math.abs(cell.hashCode()))
				));
			})
			.post("/login", ctx -> {
				assertEquals("22", ctx.cookie("Cookie2"));

				final Map<String, String> data = ctx.bodyAsClass(Map.class);
				log("Login - {}", data);

				ctx.json(asMap(
					pair("token", UUID.randomUUID().toString())
				));
			});
	}

	private static Map<String, Object> asMap(Pair... pairs) {
		final Map<String, Object> result = new HashMap<>();
		for (Pair pair : pairs) {
			result.put(pair.getKey(), pair.getValue());
		}
		return result;
	}

	private static void log(String str, Object... vars) {
		log.info("--- TEST --- | " + str, vars);
	}
}
