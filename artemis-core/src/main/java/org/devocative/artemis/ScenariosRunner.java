package org.devocative.artemis;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.devocative.artemis.cfg.Config;
import org.devocative.artemis.ctx.StatisticsContext;
import org.devocative.artemis.http.*;
import org.devocative.artemis.log.ALog;
import org.devocative.artemis.util.Immutable;
import org.devocative.artemis.util.Parallel;
import org.devocative.artemis.xml.*;
import org.devocative.artemis.xml.method.XBody;
import org.devocative.artemis.xml.method.XWhen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.devocative.artemis.EVarScope.*;
import static org.devocative.artemis.Memory.EStep.*;
import static org.devocative.artemis.util.Util.asMap;

public class ScenariosRunner implements Runnable {
	private static final String THIS = "_this";
	private static final String PREV = "_prev";
	private static final String G_LOOP_VAR = "_gLoop";
	private static final String LOOP_VAR = "_loop";

	// ------------------------------

	private final List<XScenario> scenarios;
	private final List<XVar> globalVars;
	private final Config config;

	// ------------------------------

	public ScenariosRunner(List<XScenario> scenarios, List<XVar> globalVars, Config config) {
		this.scenarios = scenarios;
		this.globalVars = globalVars;
		this.config = config;
	}

	// ------------------------------

	@Override
	public void run() {
		ALog.info("*---------*---------*");
		ALog.info("|   A R T E M I S   |");
		ALog.info(config.getDevMode() ? "*-------D E V-------*" : "*---------*---------*");

		final int loopMax = config.getLoop();
		ContextHandler.createContext();

		long start = System.currentTimeMillis();
		int iteration = 0;
		boolean successfulExec = true;

		try {
			for (; iteration < loopMax; iteration++) {
				start = System.currentTimeMillis();

				final Context ctx = ContextHandler.get();

				globalVars.forEach(var -> {
					final String value = var.getValue();
					ctx.addVarByScope(var.getName(), value, EVarScope.Global);
					ALog.info("%cyan(Global Var:) name=[{}] value=[{}]", var.getName(), value);
				});

				for (XScenario scenario : scenarios) {
					final String parallel = scenario.getParallel();
					int parallelValue = parallel != null ? Integer.parseInt(parallel) : 1;

					if (parallelValue > 1) {
						final int itr = iteration;
						ALog.info("%purple(=============== [{} - PARALLEL] ===============)", scenario.getId());

						final Runnable runnable = () -> {
							ContextHandler.createContext(ctx);

							final long startScenario = System.currentTimeMillis();
							runScenario(scenario, itr);
							final long duration = System.currentTimeMillis() - startScenario;

							StatisticsContext.execFinished(itr, duration, "");
							StatisticsContext.printThis(duration);
						};

						final Result result = Parallel.execute(
							Thread.currentThread().getName() + "_" + scenario.getId(),
							parallelValue,
							runnable);
						if (result.hasError()) {
							throw new TestFailedException(result.getErrors()).setDegree(result.getDegree()).setNoOfErrors(result.getNoOfErrors());
						}
					} else {
						runScenario(scenario, iteration);
					}
				}

				final long duration = System.currentTimeMillis() - start;
				if (loopMax == 1) {
					StatisticsContext.printThis(duration);
				} else {
					ALog.info("%green***** [PASSED SUCCESSFULLY in {} ms, loopIdx={}] *****)", duration, iteration);
				}

				StatisticsContext.execFinished(iteration, duration, "");
			}
		} catch (RuntimeException e) {
			successfulExec = false;
			ALog.error(e.getMessage());
			ContextHandler.memorize();
			StatisticsContext.execFinished(iteration, System.currentTimeMillis() - start, e.getMessage());
			throw e;
		} finally {
			ContextHandler.shutdown(successfulExec);
			HttpFactory.shutdown();
		}
	}

	private void runScenario(XScenario scenario, int iteration) {
		final Context ctx = ContextHandler.get();

		ctx.addVarByScope(G_LOOP_VAR, iteration, Global);

		ContextHandler.updateMemory(m -> m.setScenarioName(scenario.getId()));

		scenario.updateRequestsIds();

		final String loopMaxStr = scenario.getLoop();
		final int loopMax = loopMaxStr == null ? 1 : Integer.parseInt(loopMaxStr);

		for (int it = 0; it < loopMax; it++) {
			if (loopMax == 1) {
				ALog.info("%purple(=============== [{}] ===============)", scenario.getId());
			} else {
				ALog.info("%purple(=============== [{}]_{}/{}_===============)", scenario.getId(), it + 1, loopMax);
			}
			scenario.getVars().forEach(v -> ctx.addVarByScope(v.getName(), v.getValue(), Scenario));

			ctx.addVarByScope(LOOP_VAR, it, Scenario);

			if (scenario.getCall() != null && scenario.getCall()) {
				try {
					ctx.runAtScope(Scenario, () -> ContextHandler.invoke(scenario.getId()));
					ALog.info("%cyan(Call Method) - '{}(Context)'", scenario.getId());
				} catch (RuntimeException e) {
					ALog.error("ERROR: Scenario({}) - calling method: '{}(Context)'", scenario.getId(), scenario.getId());
					throw e;
				}
			}

			for (XBaseRequest rq : scenario.getRequests()) {
				ALog.info("%blue(--------------- [{}] ---------------)", rq.getId());

				ContextHandler.updateMemory(m -> m.setRqId(rq.getId()));

				if (!XBaseRequest.BREAK_POINT_ID.equals(rq.getId())) {
					if (evaluateWhen(rq.getWhen())) {
						initRq(rq);
						sendRq(rq);

						checkSleep(scenario);

						ContextHandler.updateMemory(Memory::clear);
						ctx.clearVars(EVarScope.Request);
					} else {
						final String msg = rq.getWhen().getMessage() != null ? rq.getWhen().getMessage() : "'when' is false!";
						ALog.info("RQ SKIPPED: {}", msg);
					}
				} else if (config.getDevMode()) {
					throw new TestFailedException("Reached Break Point!");
				} else {
					ALog.warn("Passing <break-point/> in Main Mode!");
				}
			}
		}

		ctx.clearVars(Scenario);
		ContextHandler.updateMemory(Memory::clearAll);
	}

	private Boolean evaluateWhen(XWhen when) {
		if (when != null) {
			return (Boolean) ContextHandler.evalExpr(when.getContent());
		}
		return true;
	}

	private void initRq(XBaseRequest rq) {
		final Context ctx = ContextHandler.get();

		if (ctx.containsVar(THIS, Scenario)) {
			ctx.addVarByScope(PREV, ctx.removeVar(THIS, Scenario), Scenario);
		}

		ContextHandler.updateMemory(m -> m.addStep(RqVars));

		int addVars = 0;
		for (XVar var : rq.getVars()) {
			ctx.addVarByScope(var.getName(), var.getValue(), Request);
			addVars++;
		}
		if (addVars > 0) {
			ALog.info("[{}] var(s) added to context", addVars);
		}

		ContextHandler.updateMemory(m -> m.addStep(RqCall));
		if (rq.getCall() != null && rq.getCall()) {
			if (!rq.isWithId()) {
				throw new TestFailedException(rq.getId(), "No id for Request to Call");
			}
			try {
				ctx.runAtScope(Request, () -> ContextHandler.invoke(rq.getId()));
				ALog.info("%cyan(Call Method) - '{}(Context)'", rq.getId());
			} catch (RuntimeException e) {
				ALog.error("ERROR: RQ({}) - calling method: '{}(Context)'", rq.getId(), rq.getId());
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

		final HttpRequestData data = new HttpRequestData(rq.getId(), rq.getUrl(), rq.getMethod().name());
		data.setHeaders(asMap(rq.getHeaders()));

		final XBody body = rq.getBody();
		if (body != null) {
			data.setBody(body.getContent().trim());
			rqAndRs.put("oRq", body.getContent().trim());
		}

		if (rq.getForm() != null) {
			data.setFormFields(rq.getForm().stream().map(x -> new FormField(x.getName(), x.getValue(), x.isFile())).collect(Collectors.toList()));
		}

		ContextHandler.getAspects().callBeforeSend(data);

		final HttpRequest httpRq = HttpFactory.create(rq);
		httpRq.setHeaders(data.getHeaders());
		if (data.getBody() != null) {
			httpRq.setBody(data.getBody().toString());
		}
		httpRq.setFormParams(data.getFormFields());
		httpRq.send(rs -> processRs(rs, rq, rqAndRs));
	}

	private void processRs(HttpResponse rs, XBaseRequest rq, Map<String, Object> rqAndRs) {

		if (rq.getAssertRs() == null) {
			ALog.warn("RQ({}) - No <assertRs/>!", rq.getId());
			rq.setAssertRs(new XAssertRs());
		}

		final XAssertRs assertRs = rq.getAssertRs();
		if (assertRs.getBody() == null) {
			assertRs.setBody(ERsBodyType.json);
		}
		assertCode(rq, rs);

		if (assertRs.getProperties() != null && assertRs.getBody() != ERsBodyType.json) {
			throw new TestFailedException(rq.getId(), "Invalid <assertRs/> Definition: properties defined for non-json body");
		}

		final String rsBodyAsStr = rs.getBody();
		switch (assertRs.getBody()) {
			case json:
				final Object obj = json(rq.getId(), rsBodyAsStr);
				rqAndRs.put("rs", obj);
				if (obj instanceof Map) {
					ALog.info("%cyan(RS Properties =) {}", ((Map<?, ?>) obj).keySet());
				}
				assertProperties(rq, obj);
				if (assertRs.getStore() != null) {
					if (rq.isWithId()) {
						storeProperties(rq.getId(), assertRs.getStore(), obj);
					} else {
						throw new TestFailedException(rq.getId(), "Id Not Found to Store: %s", assertRs.getStore());
					}
				}

				ContextHandler.getAspects().callCommonAssertRs(rq.getId(), obj);

				if (assertRs.getCall() != null && assertRs.getCall()) {
					if (rq.isWithId()) {
						assertCall(rq, obj);
					} else {
						throw new TestFailedException(rq.getId(), "Id Not Found to Call Assert");
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

		assertCookies(rs, rq, assertRs);
	}

	private void storeProperties(String id, String properties, Object rsAsObj) {
		if (rsAsObj instanceof Map) {
			final Map<?, ?> rsAsMap = (Map<?, ?>) rsAsObj;
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

	private void assertProperties(XBaseRequest rq, Object rsAsObj) {
		final XAssertRs assertRs = rq.getAssertRs();

		if (assertRs.getProperties() != null) {
			final String[] properties = assertRs.getProperties().split(",");

			if (rsAsObj instanceof Map) {
				final Map<?, ?> rsAsMap = (Map<?, ?>) rsAsObj;
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
			throw new TestFailedException(rq.getId(), "Invalid RS Code: expected %s, got %s", assertRs.getStatus(), rs.getCode());
		}
	}

	private void assertCall(XBaseRequest rq, Object obj) {
		final String methodName = String.format("assertRs_%s", rq.getId());
		if (obj instanceof Map) {
			ALog.info("%cyan(AssertRs Call:) {}(Context, Map)", methodName);
			ContextHandler.get().runAtScope(Assert, () -> ContextHandler.invoke(methodName, Immutable.create((Map) obj)));
		} else if (obj instanceof List) {
			ALog.info("%cyan(AssertRs Call:) {}(Context, List)", methodName);
			ContextHandler.get().runAtScope(Assert, () -> ContextHandler.invoke(methodName, Immutable.create((List<?>) obj)));
		} else {
			throw new TestFailedException(rq.getId(), "Unsupported Response Body Type");
		}
	}

	private void assertCookies(HttpResponse rs, XBaseRequest rq, XAssertRs assertRs) {
		if (!rs.getCookies().isEmpty() && assertRs.getCookies() != null) {
			final String[] cookieNames = assertRs.getCookies().split(",");
			for (String cookieName : cookieNames) {
				if (!rs.getCookies().containsKey(cookieName.trim())) {
					throw new TestFailedException(rq.getId(), "Cookie Not Found: %s", cookieName);
				}
			}
		}
	}

	private Object findValue(String[] parts, int idx, Map<?, ?> rsAsMap) {
		final Object obj = rsAsMap.get(parts[idx]);

		if (obj == null) {
			throw new RuntimeException(String.format("Prop Not Found: %s (%s)", parts[idx], String.join(".", parts)));
		}

		if (idx == parts.length - 1) {
			return obj;
		} else {
			final Map<String, Object> result = new HashMap<>();
			if (!(obj instanceof Map)) {
				throw new RuntimeException(String.format("Invalid Prop as Map: %s (%s)", parts[idx], String.join(".", parts)));
			}
			result.put(parts[idx], findValue(parts, idx + 1, (Map<?, ?>) obj));
			return result;
		}
	}

	private Object json(String id, String content) {
		try {
			return ContextHandler.fromJson(content, Object.class);
		} catch (JsonProcessingException e) {
			throw new TestFailedException(id, "Invalid JSON Format:\n%s", content);
		}
	}

	private void checkSleep(XScenario scenario) {
		final String sleepStr = scenario.getSleep();
		if (sleepStr != null) {
			long sleep = Long.parseLong(sleepStr);
			try {
				ALog.info("sleep: {}", sleep);
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				throw new RuntimeException("Sleep Problem", e);
			}
		}
	}
}
