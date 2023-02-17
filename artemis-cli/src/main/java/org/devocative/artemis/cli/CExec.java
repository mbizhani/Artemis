package org.devocative.artemis.cli;

import org.devocative.artemis.ArtemisExecutor;
import org.devocative.artemis.cfg.Config;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;

import static org.devocative.artemis.util.Util.isEmpty;

@Command(name = "exec", description = "Execute Artemis", mixinStandardHelpOptions = true)
public class CExec implements Runnable {

	@Parameters(arity = "0..1", paramLabel = "BaseURL", description = "base url for all requests")
	private String baseUrl;

	@Option(names = {"-n", "--name"}, paramLabel = "NAME", description = "look for '<name>.xml' and '<name>.groovy'")
	private String name;

	@Option(names = {"-x", "--xml"}, paramLabel = "XmlFileName", description = "look for '<name>.xml'")
	private String xmlName;

	@Option(names = {"-g", "--groovy"}, paramLabel = "GroovyFileName", description = "look for '<name>.groovy'")
	private String groovyName;

	@Option(names = {"-d", "--dir"}, paramLabel = "BaseDirectory", description = "look for files in this directory")
	private String baseDir;

	@Option(names = {"-p", "--parallel"}, paramLabel = "ParallelExecution", description = "create threads to execute concurrently")
	private Integer parallel;

	@Option(names = {"-l", "--loop"}, paramLabel = "Loop", description = "number of serial execution")
	private Integer loop;

	@Option(names = {"-v", "--var"}, paramLabel = "VariablesInsideTest", split = ",", splitSynopsisLabel = "=", description = "external variables(s) passing to test")
	private Map<String, String> vars;

	@Option(names = {"-X", "--proxy"}, paramLabel = "HTTP/SocksProxy", description = "all requests passed through the proxy")
	private String proxy;

	// ------------------------------

	@Override
	public void run() {
		final Config config = ConfigLoader.load(name, xmlName, groovyName);

		if (!isEmpty(baseUrl)) {
			config.setBaseUrl(baseUrl);
		}
		if (!isEmpty(baseDir)) {
			config.setBaseDir(baseDir);
		}
		if (loop != null) {
			config.setLoop(loop);
		}
		if (parallel != null) {
			config.setParallel(parallel);
		}
		if (!isEmpty(proxy)) {
			config.setProxy(proxy);
		}

		if (vars != null) {
			vars.forEach(config::addVar);
		}

		try {
			ArtemisExecutor.run(config);
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			if (e.getCause() != null) {
				System.err.println("ERROR: " + e.getCause().getMessage());
			} else {
				System.err.println("ERROR: " + e.getMessage());
			}
		}
	}
}
