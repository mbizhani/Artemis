package org.devocative.artemis.cli;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Command(name = "init", description = "Create Artemis Files", mixinStandardHelpOptions = true)
public class CInit implements Runnable {

	@Override
	public void run() {
		final InputStream xmlIS = CInit.class.getResourceAsStream("/_template_.xml");
		final InputStream groovyIS = CInit.class.getResourceAsStream("/_template_.groovy");
		final InputStream configIS = CInit.class.getResourceAsStream("/_template_config_.yaml");

		final File baseDir = new File("files");
		if (baseDir.exists()) {
			if (!baseDir.isDirectory()) {
				throw new RuntimeException("'files' should be a directory!");
			}
		} else {
			baseDir.mkdir();
		}

		try {
			final File xml = new File("files/artemis.xml");
			if (xml.exists()) {
				log.info("File Already Exists: files/artemis.xml");
			} else {
				final Path xmlPath = Paths.get(baseDir.getAbsolutePath(), "artemis.xml");
				Files.copy(xmlIS, xmlPath);
				log.info("File Created: {}", xmlPath);
			}

			final File groovy = new File("files/artemis.groovy");
			if (groovy.exists()) {
				log.info("File Already Exists: files/artemis.groovy");
			} else {
				final Path groovyPath = Paths.get(baseDir.getAbsolutePath(), "artemis.groovy");
				Files.copy(groovyIS, groovyPath);
				log.info("File Created: {}", groovyPath);
			}

			final File config = new File("config.yaml");
			if (config.exists()) {
				log.info("File Already Exists: config.yaml");
			} else {
				final Path configPath = Paths.get(baseDir.getAbsolutePath(), "..", "config.yaml");
				Files.copy(configIS, configPath);
				log.info("File Created: {}", configPath.normalize());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
