package org.devocative.artemis.test;

import io.javalin.Javalin;
import org.devocative.artemis.ArtemisMain;
import org.devocative.artemis.ContextHandler;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.devocative.artemis.test.Pair.pair;

public class TestArtemis {

	public static void main(String[] args) throws Exception {

		/*ContextHandler.get().addVar("a", 23);
		System.out.println(ContextHandler.eval("ASD: ${_.genNationalId()}"));
		System.out.println("TestArtemis.main");

		if(1==1) {
			return;
		}*/

		if ("local".equals(ContextHandler.get().getProfile())) {
			final Javalin app = Javalin
				.create()
				.start(8080);

			try {
				app
					.post("/auth/sms-code/mobile/:mobile", ctx -> {
						ctx.json(asMap(
							pair("smsCode", Math.abs(ctx.pathParam("mobile").hashCode()))
						));
					})
					.post("/auth/sms-code/verify/:mobile", ctx -> {
						ctx.json(asMap(
							pair("token", UUID.randomUUID().toString())
						));
					});

				ArtemisMain.run();
			} finally {
				app.stop();
			}
		} else {
			ArtemisMain.run();
		}
	}

	private static Map<String, Object> asMap(Pair... pairs) {
		return Arrays.stream(pairs).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}
}
