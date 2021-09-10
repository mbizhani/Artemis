package org.devocative.artemis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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

	// ------------------------------

	@Override
	public void execute() throws MojoExecutionException {
		logger.info("Run Artemis: Name=[{}], DevMode=[{}], BaseUrl=[{}]", name, devMode, baseUrl);

		ArtemisExecutor.run(new Config(name)
			.setBaseUrl(baseUrl)
			.setBaseDir(baseDir)
			.setDevMode(devMode));
	}
}
