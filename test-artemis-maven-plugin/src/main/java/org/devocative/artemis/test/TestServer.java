package org.devocative.artemis.test;

import io.javalin.Javalin;
import io.javalin.core.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.devocative.artemis.test.Pair.pair;

public class TestServer {
	private static final Logger log = LoggerFactory.getLogger(TestServer.class);

	public static void main(String[] args) {
		final Javalin app = Javalin
			.create()
			.start(8080);

		configure(app);
	}

	private static void configure(Javalin app) {
		app
			.post("/registrations", ctx -> {
				final Validator<String> _p = ctx
					.queryParam("_p", String.class)
					.check(s -> s.length() == 3);
				final Validator<Integer> p1 = ctx.queryParam("p1", Integer.class)
					.check(i -> i > 0);

				final Map<String, String> data = ctx.bodyAsClass(Map.class);
				log("Register (Sending SMS) - {}, _p={}, p1={}", data, _p.get(), p1.get());

				if (_p.hasError() || p1.hasError()) {
					ctx.status(400);
				}
			})
			.get("/registrations/:cell", ctx -> {
				final String cell = ctx.pathParam("cell");
				log("Query (Sent SMS) - {}", cell);

				final Validator<Integer> p1 = ctx.queryParam("p1", Integer.class)
					.check(i -> i > 0);

				if (p1.hasError()) {
					ctx.status(400);
				} else {
					ctx.json(asMap(
						pair("smsCode", Math.abs(cell.hashCode()))
					));
				}
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
				final Validator<String> city = ctx.formParam("city", String.class)
					.check(s -> s.startsWith("artemis"));
				final Validator<String> email = ctx.formParam("email", String.class)
					.check(s -> s.matches("\\w+@(\\w+\\.)+\\w+"));

				log("UpdateProfile - id=[{}], authHeader=[{}], city={}, email={}",
					ctx.pathParam("id"), ctx.header("Authorization"), city.get(), email.get());

				if (city.hasError() || email.hasError()) {
					ctx.status(400);
				}
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