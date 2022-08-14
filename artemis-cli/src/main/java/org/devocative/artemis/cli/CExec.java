package org.devocative.artemis.cli;

import org.devocative.artemis.ArtemisExecutor;
import org.devocative.artemis.cfg.Config;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.stream.Stream;

@Command(name = "exec", description = "Execute Artemis", mixinStandardHelpOptions = true)
public class CExec implements Runnable {

	@Parameters(index = "0", arity = "0..1", paramLabel = "BaseURL", description = "base url for all requests",
		defaultValue = "http://localhost:8080")
	private String baseUrl;

	@Option(names = {"-n", "--name"}, paramLabel = "NAME", description = "look for '<name>.xml' and '<name>.groovy'",
		defaultValue = "artemis")
	private String name;

	@Option(names = {"-x", "--xml"}, paramLabel = "XmlFileName", description = "look for '<name>.xml'")
	private String xmlName;

	@Option(names = {"-g", "--groovy"}, paramLabel = "GroovyFileName", description = "look for '<name>.groovy'")
	private String groovyName;

	@Option(names = {"-d", "--dir"}, paramLabel = "BaseDirectory", description = "look for files in this directory",
		defaultValue = ".")
	private String baseDir;

	@Option(names = {"-p", "--parallel"}, paramLabel = "ParallelExecution", description = "create threads to execute concurrently",
		defaultValue = "1")
	private Integer parallel;

	@Option(names = {"-l", "--loop"}, paramLabel = "Loop", description = "number of serial execution", defaultValue = "1")
	private Integer loop;

	@Option(names = {"-v", "--var"}, paramLabel = "VariablesInsideTest", split = ",", description = "external variables(s) passing to test")
	private String[] vars;

	@Option(names = {"-X", "--proxy"}, paramLabel = "HTTP/SocksProxy", description = "all requests passed through the proxy")
	private String proxy;

	// ------------------------------

	@Override
	public void run() {
		if (xmlName == null) {
			xmlName = name;
		}

		if (groovyName == null) {
			groovyName = name;
		}

		final Config config = new Config(xmlName, groovyName);
		config
			.setBaseUrl(baseUrl)
			.setBaseDir(baseDir)
			.setLoop(loop)
			.setParallel(parallel)
			.setProxy(proxy);

		if (vars != null) {
			Stream.of(vars)
				.map(v -> v.split("="))
				.filter(arr -> arr.length == 2)
				.forEach(arr -> config.addVar(arr[0], arr[1]));
		}

		try {
			ArtemisExecutor.run(config);
		} catch (Exception e) {
			if (e.getCause() != null) {
				System.err.println("ERROR: " + e.getCause().getMessage());
			} else {
				System.err.println("ERROR: " + e.getMessage());
			}
		}
	}
}
