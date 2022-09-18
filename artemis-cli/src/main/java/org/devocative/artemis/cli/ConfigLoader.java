package org.devocative.artemis.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.devocative.artemis.cfg.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.devocative.artemis.Util.isEmpty;

@Slf4j
public class ConfigLoader {

	public static Config load(String name, String xmlName, String groovyName) {
		final ConfigYAML yaml = readYAML();

		if (!isEmpty(name)) {
			yaml.name = name;
		}

		if (isEmpty(yaml.name)) {
			yaml.name = "artemis";
		}

		if (isEmpty(yaml.xmlName)) {
			yaml.xmlName = yaml.name;
		}

		if (isEmpty(yaml.groovyName)) {
			yaml.groovyName = yaml.name;
		}

		if (!isEmpty(xmlName)) {
			yaml.xmlName = xmlName;
		}

		if (!isEmpty(groovyName)) {
			yaml.groovyName = groovyName;
		}

		final Config config = new Config(yaml.xmlName, yaml.groovyName);
		config
			.setBaseUrl(yaml.baseUrl)
			.setBaseDir(yaml.baseDir)
			.setLoop(yaml.loop != null ? yaml.loop : 1)
			.setParallel(yaml.parallel != null ? yaml.parallel : 1)
			.setProxy(yaml.proxy);

		if (yaml.vars != null) {
			yaml.vars.stream()
				.filter(var -> !isEmpty(var.name))
				.forEach(var -> config.addVar(var.name, var.value));
		}

		return config;
	}

	private static ConfigYAML readYAML() {
		final File config = new File("config.yaml");

		if (config.exists()) {
			final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			try {
				return mapper.readValue(config, ConfigYAML.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			log.warn("File Not Found: 'config.yaml'");
		}
		return new ConfigYAML();
	}

	@Getter
	@Setter
	private static class ConfigYAML {
		private String baseUrl;
		private String name;
		private String xmlName;
		private String groovyName;
		private String baseDir;
		private Integer parallel;
		private Integer loop;
		private String proxy;
		private List<Var> vars;
	}

	@Getter
	@Setter
	private static class Var {
		private String name;
		private String value;
	}
}
