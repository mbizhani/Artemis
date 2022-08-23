package org.devocative.artemis.log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;

class CustomOutputLayout extends LayoutBase<ILoggingEvent> {

	private boolean outputPatternAsHeader;
	private boolean showANSIFormats;
	private String pattern;

	CustomOutputLayout(boolean showANSIFormats) {
		this.showANSIFormats = showANSIFormats;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public void setOutputPatternAsHeader(boolean outputPatternAsHeader) {
		this.outputPatternAsHeader = outputPatternAsHeader;
	}

	@Override
	public String doLayout(ILoggingEvent event) {
		PatternLayout pl = new PatternLayout();
		pl.setContext(context);
		pl.setPattern(pattern);
		pl.setOutputPatternAsHeader(outputPatternAsHeader);
		pl.start();
		String formattedString = pl.doLayout(event);
		MessageParser messageParser = new MessageParser(this.showANSIFormats);
		return messageParser.parseFormatters(formattedString);
	}
}
