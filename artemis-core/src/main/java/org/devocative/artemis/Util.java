package org.devocative.artemis;

import org.devocative.artemis.xml.INameTheValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Util {
	public static Map<String, String> asMap(List<? extends INameTheValue> list) {
		return list == null ? Collections.emptyMap() :
			list
				.stream()
				.collect(Collectors.toMap(INameTheValue::getName, INameTheValue::getValue));
	}
}
