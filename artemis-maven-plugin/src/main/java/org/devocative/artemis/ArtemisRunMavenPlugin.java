package org.devocative.artemis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	// ------------------------------

	@Override
	public void execute() {
		logger.info("Run Artemis: Name=[{}], DevMode=[{}], Parallel=[{}], BaseUrl=[{}]",
			name, devMode, parallel, baseUrl);

		ArtemisExecutor.run(new Config(name)
			.setBaseUrl(baseUrl)
			.setBaseDir(baseDir)
			.setDevMode(devMode)
			.setParallel(parallel)
			.setLoop(loop));
	}
}
