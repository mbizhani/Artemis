package org.devocative.artemis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.devocative.artemis.cfg.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

@Mojo(name = "run", requiresProject = false)
public class ArtemisRunMavenPlugin extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(ArtemisRunMavenPlugin.class);
	private static final String TEST_RESOURCE_DIR = "src/test/resources/";

	@Parameter(property = "name", defaultValue = "artemis")
	private String name;

	@Parameter(property = "xmlName")
	private String xmlName;

	@Parameter(property = "groovyName")
	private String groovyName;

	@Parameter(property = "baseUrl", defaultValue = "http://localhost:8080")
	private String baseUrl;

	@Parameter(property = "devMode", defaultValue = "false")
	private Boolean devMode;

	@Parameter(property = "baseDir")
	private String baseDir;

	@Parameter(property = "parallel", defaultValue = "1")
	private Integer parallel;

	@Parameter(property = "loop", defaultValue = "1")
	private Integer loop;

	@Parameter
	private Var[] vars;

	@Parameter
	private String proxy;

	// ------------------------------

	@Override
	public void execute() {
		if (baseDir == null) {
			baseDir = Files.exists(Paths.get("pom.xml")) ? TEST_RESOURCE_DIR : ".";
		}

		logger.info("Run Artemis: baseDir=[{}]", Paths.get(baseDir).toAbsolutePath().normalize());

		if (xmlName == null) {
			xmlName = name;
		}

		if (groovyName == null) {
			groovyName = name;
		}

		logger.info("Run Artemis: XMLName=[{}], GroovyName=[{}] DevMode=[{}], Parallel=[{}], BaseUrl=[{}], Vars={}",
			xmlName, groovyName, devMode, parallel, baseUrl, Arrays.toString(vars));

		final Config config = new Config(xmlName, groovyName);
		config
			.setBaseUrl(baseUrl)
			.setBaseDir(baseDir)
			.setDevMode(devMode)
			.setParallel(parallel)
			.setLoop(loop)
			.setProxy(proxy);

		if (vars != null) {
			Stream.of(vars).forEach(v -> config.addVar(v.getName(), v.getValue()));
		}

		ArtemisExecutor.run(config);
	}

	// ------------------------------

	public static class Var {
		private String name;
		private String value;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("'%s': '%s'", getName(), getValue());
		}
	}
}
