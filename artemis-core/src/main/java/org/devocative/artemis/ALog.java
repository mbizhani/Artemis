package org.devocative.artemis;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import ch.qos.logback.core.sift.Discriminator;
import ch.qos.logback.core.util.FileSize;

public class ALog {
	private static final String PATTERN = "%date %-5level - %msg%n";
	private static final LoggerContext lc = new LoggerContext();
	private static Logger log = null;

	public synchronized static void init(String name, boolean enableConsole) {
		Thread.currentThread().setName(name);

		if (log != null) {
			return;
		}

		lc.start();

		final Discriminator<ILoggingEvent> discriminator = new AbstractDiscriminator<ILoggingEvent>() {
			@Override
			public String getDiscriminatingValue(ILoggingEvent iLoggingEvent) {
				return Thread.currentThread().getName();
			}

			@Override
			public String getKey() {
				return null;
			}
		};
		discriminator.start();

		final SiftingAppender sa = new SiftingAppender();
		sa.setContext(lc);
		sa.setName("Artemis");
		sa.setDiscriminator(discriminator);
		sa.setAppenderFactory((context, discriminatingValue) -> {
			final PatternLayoutEncoder ple = new CustomLayoutEncoder(new FileLayout());
			ple.setContext(context);
			ple.setPattern(PATTERN);
			ple.start();

			final RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
			appender.setContext(context);
			appender.setName(discriminatingValue);
			appender.setFile("logs/" + discriminatingValue + ".log");
			appender.setEncoder(ple);

			final SizeAndTimeBasedRollingPolicy<ILoggingEvent> policy = new SizeAndTimeBasedRollingPolicy<>();
			policy.setContext(context);
			policy.setParent(appender);
			policy.setMaxHistory(5);
			policy.setFileNamePattern("logs/" + discriminatingValue + "-%d{yyyy-MM-dd-HH}-%i.log");
			policy.setMaxFileSize(FileSize.valueOf("5mb"));
			policy.start();

			appender.setRollingPolicy(policy);
			appender.start();

			return appender;
		});
		sa.start();

		final Logger logger = lc.getLogger(ALog.class);
		logger.setLevel(Level.INFO);
		logger.setAdditive(false);
		logger.addAppender(sa);

		if (enableConsole) {
			final PatternLayoutEncoder ple = new CustomLayoutEncoder(new ConsoleLayout());
			ple.setContext(lc);
			ple.setPattern(PATTERN);
			ple.start();

			final ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
			ca.setContext(lc);
			ca.setEncoder(ple);
			ca.start();

			logger.addAppender(ca);
		}

		log = logger;
	}

	public static void info(String s, Object... params) {
		log.info(s, params);
	}

	public static void warn(String s, Object... params) {
		log.warn(s, params);
	}

	public static void error(String s, Object... params) {
		log.error(s, params);
	}
}

class CustomLayoutEncoder extends PatternLayoutEncoder {
	CustomOutputLayout patternLayout;

	CustomLayoutEncoder(CustomOutputLayout patternLayout) {
		this.patternLayout = patternLayout;
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

class CustomOutputLayout extends LayoutBase<ILoggingEvent> {
	private boolean outputPatternAsHeader;
	private String pattern;

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
		return pl.doLayout(event);
	}
}

class FileLayout extends CustomOutputLayout {
	@Override
	public String doLayout(ILoggingEvent event) {
		String formattedString = super.doLayout(event);
		MessageParser messageParser = new MessageParser(false);
		return messageParser.parseFormatters(formattedString);
	}
}

class ConsoleLayout extends CustomOutputLayout {
	@Override
	public String doLayout(ILoggingEvent event) {
		String formattedString = super.doLayout(event);
		MessageParser messageParser = new MessageParser(true);
		return messageParser.parseFormatters(formattedString);
	}
}

class MessageParser {
	private boolean showANSIFormats;

	MessageParser(boolean showANSIFormats) {
		this.showANSIFormats = showANSIFormats;
	}

	private static String argToAnsiFormatter(String theArg) {
		switch (theArg) {
			case "black":
				return "\u001B[30m";
			case "red":
				return "\u001B[31m";
			case "green":
				return "\u001B[32m";
			case "yellow":
				return "\u001B[33m";
			case "blue":
				return "\u001B[34m";
			case "purple":
				return "\u001B[35m";
			case "cyan":
				return "\u001B[36m";
			case "white":
				return "\u001B[37m";
			case "reset":
				return "\u001B[0m";
			default:
				return "";
		}
	}

	public String parseFormatters(String sub) {
		Integer percentIndex = sub.indexOf("%");
		if (percentIndex == -1) return sub;
		String leadingPercent = sub.substring(percentIndex + 1);
		Integer openIndex = leadingPercent.indexOf("(");
		if (openIndex == -1) return sub;
		String leadingParentheses = leadingPercent.substring(openIndex + 1);
		String inner = parseFormatters(leadingParentheses);
		Integer closeIndex = inner.indexOf(")");
		if (closeIndex == -1) return sub;
		String theArg = leadingPercent.substring(0, openIndex);
		String preMsg = sub.substring(0, percentIndex);
		String theMsg = inner.substring(0, closeIndex);
		String postMsg = inner.substring(closeIndex + 1);
		return parseFormatters(makeMessage(preMsg, theMsg, postMsg, theArg));
	}

	private String makeMessage(String preMsg, String theMsg, String postMsg, String theArg) {
		String argInterpreted = this.showANSIFormats ? argToAnsiFormatter(theArg) : "";
		String closeInterpreted = this.showANSIFormats ? argToAnsiFormatter("reset") : "";
		return preMsg + argInterpreted + theMsg + closeInterpreted + postMsg;
	}
}