package org.devocative.artemis.test;

import io.javalin.Javalin;
import org.devocative.artemis.ArtemisMain;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.devocative.artemis.test.Pair.pair;

public class TestArtemis {
	public static void main(String[] args) throws Exception {
		final Javalin app = Javalin
			.create()
			.start(8080);

		app.post("/auth/sms-code/mobile/:mobile", ctx -> {
			ctx.json(asMap(pair("sms", Math.abs(ctx.pathParam("mobile").hashCode()))));
		});

		ArtemisMain.run();

		app.stop();
	}

	private static Map<String, Object> asMap(Pair... pairs) {
		return Arrays.stream(pairs).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}
}
