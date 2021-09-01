package org.devocative.artemis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thoughtworks.xstream.XStream;
import org.devocative.artemis.http.HttpFactory;
import org.devocative.artemis.http.HttpRequest;
import org.devocative.artemis.http.HttpResponse;
import org.devocative.artemis.xml.*;
import org.devocative.artemis.xml.method.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.devocative.artemis.EVarScope.*;
import static org.devocative.artemis.Memory.EStep.*;

public class ArtemisExecutor {
	private static final String DEFAULT_NAME = "artemis";
	private static final String THIS = "_this";
	private static final String PREV = "_prev";
	private static final String LOOP_VAR = "_loop";

	private final String name;
	private final Config config;
	private final HttpFactory httpFactory;

	// ------------------------------

	public static void run() {
		run(DEFAULT_NAME, new Config());
	}

	public static void run(Config config) {
		run(DEFAULT_NAME, config);
	}

	// Main
	public static void run(String name, Config config) {
		ALog.init(name);

		new ArtemisExecutor(name, config).execute();
	}

	// ------------------------------

	private ArtemisExecutor(String name, Config config) {
		this.name = name;
		this.config = config;

		ContextHandler.init(name, config);
		this.httpFactory = new HttpFactory(config.getBaseUrl());
	}

	// ------------------------------

	private void execute() {
		try {
			final XArtemis artemis = createXArtemis();

			final List<XScenario> scenarios = artemis.getScenarios()
				.stream()
				.filter(scenario -> scenario.isEnabled() &&
					(config.getOnlyScenarios().isEmpty() || config.getOnlyScenarios().contains(scenario.getName())))
				.collect(Collectors.toList());

			final Runnable runnable = () ->
				run(scenarios, artemis.getVars(), artemis.getLoopDegree());

			final Result result = Parallel.execute(name, artemis.getParallelDegree(), runnable);
			if (result.hasError()) {
				throw new TestFailedException(result.getErrors());
			}
		} finally {
			httpFactory.shutdown();
		}
	}

	private void run(List<XScenario> scenarios, List<XVar> globalVars, int loopMax) {
		ALog.info("*---------*---------*");
		ALog.info("|   A R T E M I S   |");
		if (config.getDevMode()) {
			ALog.info("*-------D E V-------*");
		} else {
			ALog.info("*---------*---------*");
		}

		for (int i = 0; i < loopMax; i++) {
			final Context ctx = ContextHandler.get();
			globalVars.forEach(var -> {
				final String value = var.getValue();
				ctx.addVarByScope(var.getName(), value, EVarScope.Global);
				ALog.info("Global Var: name=[{}] value=[{}]", var.getName(), value);
			});

			final String loopVar = Integer.toString(i);
			scenarios.forEach(scenario -> {
				ctx.addVarByScope(LOOP_VAR, loopVar, Global);

				ContextHandler.updateMemory(m -> m
					.clear()
					.setScenarioName(scenario.getName()));

				ALog.info("*** SCENARIO *** => {}", scenario.getName());
				try {
					scenario.getVars()
						.forEach(v -> ctx.addVarByScope(v.getName(), v.getValue(), Scenario));

					scenario.updateRequestsIds();

					for (XBaseRequest rq : scenario.getRequests()) {
						initRq(rq);
						sendRq(rq);
						ctx.clearVars(EVarScope.Request);
					}

					ctx.clearVars(Scenario);
				} catch (RuntimeException e) {
					ALog.error(e.getMessage());
					ContextHandler.memorize();
					throw e;
				}
			});
			ContextHandler.shutdown();
		}
	}

	private void initRq(XBaseRequest rq) {
		final Context ctx = ContextHandler.get();

		if (ctx.containsVar(THIS, Scenario)) {
			ctx.addVarByScope(PREV, ctx.removeVar(THIS, Scenario), Scenario);
		}

		ContextHandler.updateMemory(m -> m
			.clear()
			.setRqId(rq.getId())
			.addStep(RqVars));

		int addVars = 0;
		for (XVar var : rq.getVars()) {
			ctx.addVarByScope(var.getName(), var.getValue(), Request);
			addVars++;
		}
		if (addVars > 0) {
			ALog.info("RQ({}) - [{}] var(s) added to context", rq.getId(), addVars);
		}

		ContextHandler.updateMemory(m -> m.addStep(RqCall));
		if (rq.getCall() != null) {
			try {
				ctx.runAtScope(Request, () -> ContextHandler.invoke(rq.getCall()));
				ALog.info("RQ({}) - call: {}", rq.getId(), rq.getCall());
			} catch (RuntimeException e) {
				ALog.error("ERROR: RQ({}) - calling: {}", rq.getId(), rq.getCall());
				throw e;
			}
		}
	}

	private void sendRq(XBaseRequest rq) {
		ContextHandler.updateMemory(m -> m.addStep(RqSend));

		final Context ctx = ContextHandler.get();

		final Map<String, Object> rqAndRs = new HashMap<>();
		ctx.addVarByScope(THIS, rqAndRs, Scenario);
		if (rq.isWithId()) {
			ctx.addVarByScope(rq.getId(), rqAndRs, Scenario);
		}

		final HttpRequest httpRq = httpFactory.create(
			rq.getId(), rq.getMethod().name(), rq.getUrl(), asMap(rq.getUrlParams()));

		httpRq.setHeaders(asMap(rq.getHeaders()));

		final XBody body = rq.getBody();
		if (body != null) {
			httpRq.setBody(body.getContent().trim());
		}

		httpRq.setFormParams(asMap(rq.getFormParams()));

		httpRq.send(rs -> processRs(rs, rq, rqAndRs));
	}

	private void processRs(HttpResponse rs, XBaseRequest rq, Map<String, Object> rqAndRs) {

		if (rq.getAssertRs() == null) {
			ALog.warn("RQ({}) - No <assertRs/>!", rq.getId());
			rq.setAssertRs(new XAssertRs());
		}

		final XAssertRs assertRs = rq.getAssertRs();
		assertCode(rq, rs);

		if (assertRs.getProperties() != null) {
			if (assertRs.getBody() == null) {
				assertRs.setBody(ERsBodyType.json);
			} else if (assertRs.getBody() != ERsBodyType.json) {
				throw new TestFailedException(rq.getId(), "Invalid Assert Rs Definition: properties defined for non-json body");
			}
		}

		final String rsBodyAsStr = rs.getBody();
		if (assertRs.getBody() != null) {
			switch (assertRs.getBody()) {
				case json:
					final Object obj = json(rq.getId(), rsBodyAsStr);
					rqAndRs.put("rs", obj);
					assertProperties(rq, obj);
					if (assertRs.getStore() != null) {
						if (rq.isWithId()) {
							storeProperties(rq.getId(), assertRs.getStore(), obj);
						} else {
							throw new TestFailedException(rq.getId(), "Id Not Found to Store: %s", assertRs.getStore());
						}
					}
					break;
				case text:
					if (rsBodyAsStr.trim().isEmpty()) {
						throw new TestFailedException(rq.getId(), "Invalid Rs Body: expecting text, got empty");
					}
					rqAndRs.put("rs", rsBodyAsStr);
					break;
				case empty:
					if (!rsBodyAsStr.trim().isEmpty()) {
						throw new TestFailedException(rq.getId(), "Invalid Rs Body: expecting empty, got text");
					}
					break;
			}
		}
	}

	// ---------------

	private XArtemis createXArtemis() {
		final XStream xStream = new XStream();
		XStream.setupDefaultSecurity(xStream);
		xStream.processAnnotations(new Class[]{XArtemis.class, XGet.class, XPost.class, XPut.class, XDelete.class});
		xStream.allowTypesByWildcard(new String[]{"org.devocative.artemis.xml.**"});

		final XArtemis artemis = (XArtemis) xStream.fromXML(
			ArtemisExecutor.class.getResourceAsStream(String.format("/%s.xml", name)));

		if (config.getDevMode()) {
			artemis.setLoop(1);
			artemis.setParallel(1);

			final Memory memory = ContextHandler.getMEMORY();
			if (memory.isEmpty()) {
				ALog.info("DEV MODE - Empty Memory");
			} else {
				ALog.info("DEV MODE - Memory: {} -> {}, {}", memory.getScenarioName(), memory.getRqId(), memory.getSteps());
			}

			if (memory.getScenarioName() != null) {
				artemis.setVars(Collections.emptyList());

				final Context ctx = memory.getContext();
				if (ctx.containsVar(PREV, Scenario)) {
					ctx.addVarByScope(THIS, ctx.removeVar(PREV, Scenario), Scenario);
				}

				while (!memory.getScenarioName().equals(artemis.getScenarios().get(0).getName())) {
					final XScenario removed = artemis.getScenarios().remove(0);
					ALog.info("DEV MODE - Removed Scenario: {}", removed.getName());
				}

				final XScenario scenario = artemis.getScenarios().get(0);
				scenario.updateRequestsIds();

				final List<Memory.EStep> steps = memory.getSteps();

				if (steps.contains(RqVars)) {
					ALog.info("DEV MODE - Removed Scenario Vars: {}", scenario.getName());
					scenario.setVars(Collections.emptyList());

					while (!memory.getRqId().equals(scenario.getRequests().get(0).getId())) {
						final XBaseRequest removed = scenario.getRequests().remove(0);
						ALog.info("DEV MODE - Removed Rq: {} ({})", removed.getId(), scenario.getName());
					}

					final XBaseRequest rq = scenario.getRequests().get(0);
					if (steps.contains(RqCall)) {
						ALog.info("DEV MODE - Removed Rq Vars: {} ({})", rq.getId(), scenario.getName());
						rq.setVars(Collections.emptyList());

						if (steps.contains(RqSend)) {
							ALog.info("DEV MODE - Removed Rq Call: {} ({})", rq.getId(), scenario.getName());
							rq.setCall(null);
						}
					}
				}
			}
		}

		return Proxy.create(artemis);
	}

	private void assertProperties(XBaseRequest rq, Object rsAsObj) {
		final XAssertRs assertRs = rq.getAssertRs();

		if (assertRs.getProperties() != null) {
			final String[] properties = assertRs.getProperties().split(",");

			if (rsAsObj instanceof Map) {
				final Map rsAsMap = (Map) rsAsObj;
				for (String property : properties) {
					final String prop = property.trim();
					if (!rsAsMap.containsKey(prop)) {
						throw new TestFailedException(rq.getId(), "Invalid Property in RS Object: [%s]", prop);
					}
				}
			} else {
				throw new TestFailedException(rq.getId(), "Invalid RS Type for Asserting Properties");
			}
		}
	}

	private void storeProperties(String id, String properties, Object rsAsObj) {
		if (rsAsObj instanceof Map) {
			final Map rsAsMap = (Map) rsAsObj;
			final Map<String, Object> rs = new HashMap<>();
			final String[] props = properties.split(",");

			for (String prop : props) {
				final String[] parts = prop.trim().split("[.]");
				rs.put(parts[0], findValue(parts, 0, rsAsMap));
			}

			Map<String, Object> store = new HashMap<>();
			store.put("rs", rs);
			ContextHandler.get().addVarByScope(id, store, Global);
		}
	}

	private Object findValue(String[] parts, int idx, Map rsAsMap) {
		final Object obj = rsAsMap.get(parts[idx]);

		if (obj == null) {
			throw new RuntimeException(String.format("Prop Not Found: %s (%s)",
				parts[idx],
				String.join(".", parts)));
		}

		if (idx == parts.length - 1) {
			return obj;
		} else {
			final Map<String, Object> result = new HashMap<>();
			if (!(obj instanceof Map)) {
				throw new RuntimeException(String.format("Invalid Prop as Map: %s (%s)",
					parts[idx], String.join(".", parts)));
			}
			result.put(parts[idx], findValue(parts, idx + 1, (Map) obj));
			return result;
		}
	}

	private void assertCode(XBaseRequest rq, HttpResponse rs) {
		final XAssertRs assertRs = rq.getAssertRs();
		if (assertRs.getStatus() != null && !assertRs.getStatus().equals(rs.getCode())) {
			throw new TestFailedException(rq.getId(), "Invalid RS Code: expected %s, got %s",
				assertRs.getStatus(), rs.getCode());
		}
	}

	private Object json(String id, String content) {
		try {
			return ContextHandler.fromJson(content, Object.class);
		} catch (JsonProcessingException e) {
			throw new TestFailedException(id, "Invalid JSON Format:\n%s", content);
		}
	}

	private Map<String, String> asMap(List<? extends INameTheValue> list) {
		return list == null ? Collections.emptyMap() :
			list
				.stream()
				.collect(Collectors.toMap(INameTheValue::getName, INameTheValue::getValue));
	}
}
