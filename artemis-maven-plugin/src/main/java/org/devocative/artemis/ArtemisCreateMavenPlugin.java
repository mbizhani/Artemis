package org.devocative.artemis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mojo(name = "create")
public class ArtemisCreateMavenPlugin extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(ArtemisCreateMavenPlugin.class);

	@Parameter(defaultValue = "artemis")
	private String name;

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	// ------------------------------

	@Override
	public void execute() throws MojoExecutionException {
		final File basedir = project.getBasedir();

		final File testResourceDir = new File(basedir.getAbsolutePath() + "/src/test/resources/");
		if (!testResourceDir.exists()) {
			testResourceDir.mkdirs();
			logger.info("Directory Created: {}", testResourceDir.getAbsolutePath());
		}

		final InputStream xmlIS = ArtemisCreateMavenPlugin.class.getResourceAsStream("/artemis.xml");
		final InputStream groovyIS = ArtemisCreateMavenPlugin.class.getResourceAsStream("/artemis.groovy");

		try {
			final Path xmlPath = Paths.get(testResourceDir.getAbsolutePath(), name + ".xml");
			final Path groovyPath = Paths.get(testResourceDir.getAbsolutePath(), name + ".groovy");
			Files.copy(xmlIS, xmlPath);
			logger.info("File Created: {}", xmlPath.toString());
			Files.copy(groovyIS, groovyPath);
			logger.info("File Created: {}", groovyPath.toString());
		} catch (IOException e) {
			throw new MojoExecutionException("Creating File: " + e.getMessage(), e);
		}
	}
}