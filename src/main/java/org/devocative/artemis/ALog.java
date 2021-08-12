package org.devocative.artemis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ALog {
	private static final Logger log = LoggerFactory.getLogger(ALog.class);

	public static void info(String s, Object... params) {
		log.info(s, params);
	}

	public static void warn(String s, Object... params) {
		log.warn(s, params);
	}
}
