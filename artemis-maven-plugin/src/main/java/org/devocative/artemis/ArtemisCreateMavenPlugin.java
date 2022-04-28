package org.devocative.artemis;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mojo(name = "create", requiresProject = false)
public class ArtemisCreateMavenPlugin extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(ArtemisCreateMavenPlugin.class);
	private static final String TEST_RESOURCE_DIR = "src/test/resources/";

	@Parameter(property = "name", defaultValue = "artemis")
	private String name;

	@Parameter(property = "xmlName", defaultValue = "artemis")
	private String xmlName;

	@Parameter(property = "groovyName", defaultValue = "artemis")
	private String groovyName;

	// ------------------------------

	@Override
	public void execute() throws MojoExecutionException {
		final File baseDir = Files.exists(Paths.get("pom.xml")) ?
			new File(TEST_RESOURCE_DIR) :
			new File(".");

		if (!baseDir.exists()) {
			baseDir.mkdirs();
			logger.info("Directory Created: {}", baseDir.getAbsolutePath());
		}

		final InputStream xmlIS = ArtemisCreateMavenPlugin.class.getResourceAsStream("/_template_.xml");
		final InputStream groovyIS = ArtemisCreateMavenPlugin.class.getResourceAsStream("/_template_.groovy");

		if (xmlIS != null && groovyIS != null) {
			try {
				final Path xmlPath = Paths.get(baseDir.getAbsolutePath(), (name != null ? name : xmlName) + ".xml");
				final Path groovyPath = Paths.get(baseDir.getAbsolutePath(), (name != null ? name : groovyName) + ".groovy");
				Files.copy(xmlIS, xmlPath);
				logger.info("File Created: {}", xmlPath.normalize());
				Files.copy(groovyIS, groovyPath);
				logger.info("File Created: {}", groovyPath.normalize());
			} catch (IOException e) {
				throw new MojoExecutionException("Creating File: " + e.getMessage(), e);
			}
		} else {
			throw new MojoExecutionException("Artemis Template Files Not Found");
		}
	}
}
