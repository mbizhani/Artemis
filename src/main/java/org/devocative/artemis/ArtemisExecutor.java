package org.devocative.artemis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class ArtemisExecutor {
	private static final String DEFAULT_NAME = "artemis";
	private static final String THIS = "_this";
	private static final String PREV = "_prev";

	private final String name;
	private final Config config;
	private final ObjectMapper mapper;
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
		new ArtemisExecutor(name, config).execute();
	}

	// ------------------------------

	private ArtemisExecutor(String name, Config config) {
		this.name = name;
		this.config = config;
		this.mapper = new ObjectMapper();

		ContextHandler.init(name, config);
		this.httpFactory = new HttpFactory(config.getBaseUrl());
	}

	// ------------------------------

	private void execute() {
		try {
			final XArtemis artemis = createXArtemis();

			final List<XScenario> scenarios = artemis
				.getScenarios()
				.stream()
				.filter(scenario -> scenario.isEnabled() &&
					(config.getOnlyScenarios().isEmpty() || config.getOnlyScenarios().contains(scenario.getName())))
				.collect(Collectors.toList());

			final Runnable runnable = () ->
				run(scenarios, artemis.getVars(), artemis.getLoop() != null ? artemis.getLoop() : 1);

			final Result result = Parallel.execute(artemis.getParallelDegree(), runnable);
			if (result.hasError()) {
				throw new RuntimeException(result.getErrors());
			}
		} finally {
			httpFactory.shutdown();
		}
	}

	private void run(List<XScenario> scenarios, List<XVar> globalVars, int loopMax) {
		for (int i = 0; i < loopMax; i++) {
			final Context ctx = ContextHandler.get();
			globalVars.forEach(var -> {
				final String value = var.getTheValue();
				ctx.addVarByScope(var.getName(), value, EVarScope.Global);
				ALog.info("Global Var: name=[{}} value=[{}]", var.getName(), value);
			});

			final String loopVar = Integer.toString(i);
			scenarios.forEach(scenario -> {
				ctx.addVarByScope("_loop", loopVar, Global);

				ALog.info("*** SCENARIO *** => {}", scenario.getName());
				scenario.getVars().forEach(v -> ctx.addVarByScope(v.getName(), v.getTheValue(), Scenario));

				int idx = 1;
				for (XBaseRequest rq : scenario.getRequests()) {
					initRq(rq, idx++);
					sendRq(rq);
					ctx.clearVars(EVarScope.Request);
				}

				ctx.clearVars(Scenario);
			});
			ContextHandler.shutdown();
		}
	}

	private void initRq(XBaseRequest rq, int idx) {
		rq.setWithId(rq.getId() != null);
		if (rq.getId() == null) {
			rq.setId(String.format("step #%s", idx));
		}

		final Context ctx = ContextHandler.get();
		if (ctx.containsVar(THIS, Scenario)) {
			ctx.addVarByScope(PREV, ctx.removeVar(THIS, Scenario), Scenario);
		}

		int addVars = 0;
		for (XVar var : rq.getVars()) {
			ctx.addVarByScope(var.getName(), var.getTheValue(), Request);
			addVars++;
		}
		if (addVars > 0) {
			ALog.info("RQ({}) - [{}] var(s) added to context", rq.getId(), addVars);
		}

		if (rq.getCall() != null) {
			ContextHandler.invoke(rq.getCall());
			ALog.info("RQ({}) - call: {}", rq.getId(), rq.getCall());
		}
	}

	private void sendRq(XBaseRequest rq) {
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

	private void assertCode(XBaseRequest rq, HttpResponse rs) {
		final XAssertRs assertRs = rq.getAssertRs();
		if (assertRs.getStatus() != null && !assertRs.getStatus().equals(rs.getCode())) {
			throw new TestFailedException(rq.getId(), "Invalid RS Code: expected %s, got %s",
				assertRs.getStatus(), rs.getCode());
		}
	}

	private Object json(String id, String content) {
		try {
			return mapper.readValue(content, Object.class);
		} catch (JsonProcessingException e) {
			throw new TestFailedException(id, "Invalid JSON Format:\n%s", content);
		}
	}

	private Map<String, String> asMap(List<? extends INameTheValue> list) {
		return list == null ? Collections.emptyMap() :
			list
				.stream()
				.collect(Collectors.toMap(INameTheValue::getName, INameTheValue::getTheValue));
	}
}
