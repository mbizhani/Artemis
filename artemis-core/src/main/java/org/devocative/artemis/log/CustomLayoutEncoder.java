package org.devocative.artemis.log;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

class CustomLayoutEncoder extends PatternLayoutEncoder {
	CustomOutputLayout patternLayout;

	CustomLayoutEncoder(boolean showANSIFormats) {
		this.patternLayout = new CustomOutputLayout(showANSIFormats);
	}

	@Override
	public void start() {
		patternLayout.setContext(context);
		patternLayout.setPattern(getPattern());
		patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
		patternLayout.start();
		this.layout = patternLayout;
	}
}
