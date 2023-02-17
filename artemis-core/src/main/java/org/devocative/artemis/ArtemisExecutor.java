package org.devocative.artemis;

import com.thoughtworks.xstream.XStream;
import org.devocative.artemis.cfg.Config;
import org.devocative.artemis.ctx.StatisticsContext;
import org.devocative.artemis.http.HttpFactory;
import org.devocative.artemis.log.ALog;
import org.devocative.artemis.util.Parallel;
import org.devocative.artemis.util.Proxy;
import org.devocative.artemis.xml.XArtemis;
import org.devocative.artemis.xml.XBaseRequest;
import org.devocative.artemis.xml.XBreakPoint;
import org.devocative.artemis.xml.XScenario;
import org.devocative.artemis.xml.method.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.devocative.artemis.Constants.PREV;
import static org.devocative.artemis.Constants.THIS;
import static org.devocative.artemis.EVarScope.Scenario;
import static org.devocative.artemis.Memory.EStep.*;

public class ArtemisExecutor {
	private final Config config;

	// ------------------------------

	private ArtemisExecutor(Config config) {
		this.config = config;
	}

	// ------------------------------

	public static void run() {
		run(new Config());
	}

	// Main
	public static void run(Config config) {
		config.init();

		ALog.init(config.getName(), config.getConsoleLog() != null ? config.getConsoleLog() : config.getDevMode() || config.getParallel() == 1);

		ContextHandler.init(config);

		HttpFactory.init(config.getBaseUrl(), config.getProxy());

		new ArtemisExecutor(config).execute();
	}

	// ------------------------------

	private void execute() {
		final XArtemis artemis = createXArtemis();

		final List<XScenario> scenarios = artemis.getScenarios().stream()
			.filter(scenario -> scenario.isEnabled() &&
				(config.getOnlyScenarios().isEmpty() ||
					config.getOnlyScenarios().contains(scenario.getId())
				)
			)
			.collect(Collectors.toList());

		final Result result = Parallel.execute(config.getName(), config.getParallel(), new ScenariosRunner(scenarios, artemis.getVars(), config));

		StatisticsContext.printAll();

		if (result.hasError()) {
			throw new TestFailedException(result.getErrors()).setDegree(result.getDegree()).setNoOfErrors(result.getNoOfErrors());
		}
	}

	// ---------------

	private XArtemis createXArtemis() {
		final XStream xStream = new XStream();
		xStream.processAnnotations(new Class[]{XArtemis.class, XGet.class, XPost.class, XPut.class, XPatch.class, XDelete.class, XBreakPoint.class});
		xStream.allowTypesByWildcard(new String[]{"org.devocative.artemis.xml.**"});

		final XArtemis artemis = (XArtemis) xStream.fromXML(ContextHandler.loadXmlFile());

		if (config.getDevMode()) {
			if (config.getLoop() != null && config.getLoop() > 1) {
				ALog.warn("DEV MODE: 'loop' set to 1");
			}

			if (config.getParallel() != null && config.getParallel() > 1) {
				ALog.warn("DEV MODE: 'parallel' set to 1");
			}

			config.setLoop(1).setParallel(1);

			final Memory memory = ContextHandler.getMEMORY();
			if (memory.isEmpty()) {
				ALog.info("DEV MODE - Empty Memory");
			} else {
				ALog.info("DEV MODE - Memory: {} -> {}, {}", memory.getScenarioName(), memory.getRqId(), memory.getSteps());
			}

			if (memory.getScenarioName() != null) {
				artemis.setVars(Collections.emptyList());

				while (!memory.getScenarioName().equals(artemis.getScenarios().get(0).getId())) {
					final XScenario removed = artemis.getScenarios().remove(0);
					ALog.info("DEV MODE - Removed Scenario: {}", removed.getId());
				}

				final XScenario scenario = artemis.getScenarios().get(0);
				scenario.updateRequestsIds();
				ALog.info("DEV MODE - Removed Scenario Vars: {}", scenario.getId());
				scenario.setVars(Collections.emptyList());

				final List<Memory.EStep> steps = memory.getSteps();

				if (steps.contains(RqVars)) {
					final Context ctx = memory.getContext();
					if (ctx.containsVar(PREV, Scenario)) {
						ctx.addVarByScope(THIS, ctx.removeVar(PREV, Scenario), Scenario);
					}

					while (!memory.getRqId().equals(scenario.getRequests().get(0).getId())) {
						final XBaseRequest removed = scenario.getRequests().remove(0);
						ALog.info("DEV MODE - Removed Rq: {} ({})", removed.getId(), scenario.getId());
					}

					final XBaseRequest rq = scenario.getRequests().get(0);
					if (steps.contains(RqCall)) {
						ALog.info("DEV MODE - Removed Rq Vars: {} ({})", rq.getId(), scenario.getId());
						rq.setVars(Collections.emptyList());

						if (steps.contains(RqSend)) {
							ALog.info("DEV MODE - Removed Rq Call: {} ({})", rq.getId(), scenario.getId());
							rq.setCall(null);
						}
					}
				} else if (steps.isEmpty() && XBaseRequest.BREAK_POINT_ID.equals(memory.getRqId()) && memory.getLastSuccessfulRqId() != null) {

					while (!scenario.getRequests().isEmpty()) {
						final XBaseRequest removed = scenario.getRequests().remove(0);
						ALog.info("DEV MODE - Removed Rq: {} ({})", removed.getId(), scenario.getId());

						if (memory.getLastSuccessfulRqId().equals(removed.getId())) {
							break;
						}
					}
				}
			}
		}

		return Proxy.create(artemis);
	}
}
