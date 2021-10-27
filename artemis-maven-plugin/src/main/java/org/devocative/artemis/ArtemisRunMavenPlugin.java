package org.devocative.artemis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.devocative.artemis.cfg.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Stream;

@Mojo(name = "run")
public class ArtemisRunMavenPlugin extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(ArtemisRunMavenPlugin.class);

	@Parameter(property = "name", defaultValue = "artemis")
	private String name;

	@Parameter(property = "baseUrl", defaultValue = "http://localhost:8080")
	private String baseUrl;

	@Parameter(property = "devMode", defaultValue = "false")
	private Boolean devMode;

	@Parameter(property = "baseDir", defaultValue = "src/test/resources")
	private String baseDir;

	@Parameter(property = "parallel", defaultValue = "1")
	private Integer parallel;

	@Parameter(property = "loop", defaultValue = "1")
	private Integer loop;

	@Parameter
	private Var[] vars;

	// ------------------------------

	@Override
	public void execute() {
		logger.info("Run Artemis: Name=[{}], DevMode=[{}], Parallel=[{}], BaseUrl=[{}], Vars={}",
			name, devMode, parallel, baseUrl, Arrays.toString(vars));

		final Config config = new Config(name)
			.setBaseUrl(baseUrl)
			.setBaseDir(baseDir)
			.setDevMode(devMode)
			.setParallel(parallel)
			.setLoop(loop);

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
