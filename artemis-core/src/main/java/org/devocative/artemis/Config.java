package org.devocative.artemis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class Config {
	private final String name;
	private String profile;
	private String baseUrl;
	private Boolean devMode;
	private List<String> onlyScenarios = Collections.emptyList();
	private String baseDir;

	// ------------------------------

	public Config() {
		this("artemis");
	}

	public Config(String name) {
		this.name = name;
	}
}
