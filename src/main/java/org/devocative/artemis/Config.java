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
	private String profile;
	private String baseUrl;
	private List<String> onlyScenarios = Collections.emptyList();
}
