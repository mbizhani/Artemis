package org.devocative.artemis.cfg;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Var {
	private final String name;
	private final Object value;
}
