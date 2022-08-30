package org.devocative.artemis.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.javalin.Javalin;
import io.javalin.core.validation.Validator;
import io.javalin.http.UploadedFile;
import org.bbottema.javasocksproxyserver.SocksServer;
import org.devocative.artemis.ArtemisExecutor;
import org.devocative.artemis.TestFailedException;
import org.devocative.artemis.cfg.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.devocative.artemis.test.Pair.pair;
import static org.junit.jupiter.api.Assertions.*;

public class TestArtemisExecutor {
	private static final Logger log = LoggerFactory.getLogger(TestArtemisExecutor.class);

	@Test
	public void test_defaultConfig() {
		startJavalin(url -> ArtemisExecutor.run(new Config()
			.addVar("backEnd", url)
			.addVar("parallelLogin", 3)
			.addVar("loopLogin", 2)
			.setBaseUrl(url)
			.setParallel(1))
		);
	}

	@Test
	public void test_devMode_baseUrlViaConfig() {
		startJavalin(url -> ArtemisExecutor.run(new Config()
			.setDevMode(true)
			.setConsoleLog(false)
			.setBaseUrl(url)
			.setBaseDir("src/test/resources")
			.addVar("backEnd", url)
			.addVar("parallelLogin", 1)
			.addVar("loopLogin", 1))
		);
	}

	@Test
	public void test_parallel_baseUrlViaSysProp() {
		startJavalin(url -> {
			System.setProperty("artemis.base.url", url);

			ArtemisExecutor.run(new Config()
				.setParallel(8)
				.addVar("backEnd", url)
				.addVar("parallelLogin", 1)
				.addVar("loopLogin", 1));

			System.clearProperty("artemis.base.url");
		});
	}

	@Test
	public void test_error() {
		startJavalin(url -> {
			final int degree = 9;

			try {
				ArtemisExecutor.run(new Config("artemis-error")
					.addVar("backEnd", url)
					.addVar("parallelLogin", 1)
					.addVar("loopLogin", 1)
					.setBaseUrl(url)
					.setParallel(degree));

				fail();
			} catch (TestFailedException e) {
				assertEquals(degree / 2 + degree % 2, e.getNoOfErrors());
				assertEquals(degree, e.getDegree());
			} catch (Exception e) {
				log.error("test_error", e);
				fail();
			}

		});
	}

	@Test
	public void test_devMode() {
		startJavalin(url -> {
			final File memFile = new File(".artemis-devMode.memory.json");
			memFile.delete();

			try {
				ArtemisExecutor.run(new Config("artemis-devMode", "artemis")
					.setDevMode(true)
					.setBaseUrl(url)
					.addVar("backEnd", url)
					.addVar("parallelLogin", 1)
					.addVar("loopLogin", 1));

				fail();
			} catch (TestFailedException e) {
				assertEquals("TestFailedException: Reached Break Point!", e.getMessage());

				assertTrue(memFile.exists());
			} catch (Exception e) {
				log.error("test_error", e);
				fail();
			}

			memFile.delete();
		});
	}

	@Test
	public void test_socks_proxy_connection_refused() {
		startJavalin(url -> {
			try {
				ArtemisExecutor.run(new Config()
					.addVar("backEnd", url)
					.addVar("parallelLogin", 1)
					.addVar("loopLogin", 1)
					.setBaseUrl(url)
					.setProxy("socks://127.0.0.1:44444")
					.setParallel(1));

				fail();
			} catch (TestFailedException e) {
				assertEquals("TestFailedException: ERROR(registration) - CAUSE: Connection refused (Connection refused) [java.net.SocketException]", e.getMessage());
			}
		});
	}

	@Test
	public void test_socks_proxy_successful() {
		startJavalin(url -> {
			final int socksPort = findPort();
			log.info("SOCKS PROXY PORT = {}", socksPort);

			final SocksServer socksServer = new SocksServer();
			socksServer.start(socksPort);

			ArtemisExecutor.run(new Config()
				.addVar("backEnd", url)
				.addVar("parallelLogin", 1)
				.addVar("loopLogin", 1)
				.setBaseUrl(url)
				.setProxy("socks://127.0.0.1:" + socksPort)
				.setParallel(1));

			socksServer.stop();
		});
	}

	@Test
	public void test_socks_http_connection_refused() {
		startJavalin(url -> {
			try {
				ArtemisExecutor.run(new Config()
					.addVar("backEnd", "http://localhost:8080")
					.addVar("parallelLogin", 1)
					.addVar("loopLogin", 1)
					.setProxy("http://127.0.0.1:44444")
					.setParallel(1));

				fail();
			} catch (TestFailedException e) {
				assertEquals("TestFailedException: ERROR(registration) - " +
					"CAUSE: Connect to http://127.0.0.1:44444 [/127.0.0.1] failed: " +
					"Connection refused (Connection refused) [org.apache.hc.client5.http.HttpHostConnectException]", e.getMessage());
			}
		});
	}

	@Test
	public void test_http_proxy_successful() {
		startJavalin(url -> {
			final int httpPort = findPort();
			log.info("HTTP PROXY PORT = {}", httpPort);

			final WireMockServer proxyMock = new WireMockServer(wireMockConfig().port(httpPort));
			proxyMock.stubFor(
				any(urlMatching(".*"))
					.willReturn(aResponse().proxiedFrom(url))
			);
			proxyMock.start();

			ArtemisExecutor.run(new Config()
				.addVar("backEnd", url)
				.addVar("parallelLogin", 1)
				.addVar("loopLogin", 1)
				.setBaseUrl(url)
				.setProxy("http://127.0.0.1:" + httpPort)
				.setParallel(1));

			proxyMock.stop();
		});
	}

	// ------------------------------

	public static void startJavalin(Consumer<String> consumer) {
		final int port = findPort();
		log.info("Javalin PORT = {}", port);

		final Javalin app = Javalin
			.create()
			.start(port);

		app.before(context ->
			context.headerAsClass("randHead", Integer.class)
				.check(val -> {
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

				final List<String> list = ctx.queryParams("list");
				assertEquals(2, list.size());
				assertTrue(list.contains("l1"));
				assertTrue(list.contains("l2"));

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

				assertTrue(ctx.isMultipartFormData());

				final UploadedFile file = ctx.uploadedFile("logo");
				assertNotNull(file);
				assertEquals("picture.jpg", file.getFilename());
				ImageIO.read(file.getContent());

				final Validator<String> city = ctx.formParamAsClass("city", String.class)
					.check(s -> s.startsWith("artemis"), "Invalid param 'city', should starts with 'artemis'");
				final Validator<String> email = ctx.formParamAsClass("email", String.class)
					.check(s -> s.matches("\\w+@(\\w+\\.)+\\w+"), "Invalid email format");

				final List<String> groups = ctx.formParams("groups");
				assertEquals(3, groups.size());
				assertTrue(groups.contains("g1"));
				assertTrue(groups.contains("g2"));
				assertTrue(groups.contains("g3"));
				assertFalse(groups.contains("g4"));

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

		consumer.accept("http://localhost:" + port);

		app.close();
	}

	// ------------------------------

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

	private static int findPort() {
		try (final ServerSocket s = new ServerSocket(0)) {
			return s.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
