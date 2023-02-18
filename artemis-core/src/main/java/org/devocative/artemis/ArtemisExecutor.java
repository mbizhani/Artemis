package org.devocative.artemis;

import com.thoughtworks.xstream.XStream;
import org.devocative.artemis.cfg.Config;
import org.devocative.artemis.ctx.StatisticsContext;
import org.devocative.artemis.http.HttpFactory;
import org.devocative.artemis.log.ALog;
import org.devocative.artemis.util.Parallel;
import org.devocative.artemis.util.Proxy;
import org.devocative.artemis.xml.XArtemis;
import org.devocative.artemis.xml.XBreakPoint;
import org.devocative.artemis.xml.XScenario;
import org.devocative.artemis.xml.method.*;

import java.util.List;
import java.util.stream.Collectors;

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

			// Apply Memory to XArtemis object
		}

		return Proxy.create(artemis);
	}
}
