package org.devocative.artemis.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import ch.qos.logback.core.sift.Discriminator;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class ALog {
	private static final String PATTERN = "%date %-5level - %msg%n";
	private static final LoggerContext LOGGER_CONTEXT = new LoggerContext();

	private static Logger fileLog = null;
	private static Logger consoleLog = null;
	private static String name;

	// ------------------------------

	static {
		final org.slf4j.Logger logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		if (logger instanceof Logger) {
			final Logger rootLogger = (Logger) logger;
			rootLogger.setLevel(Level.ERROR);
		}
	}

	// ------------------------------

	public synchronized static void init(String name, boolean enableConsole) {
		ALog.name = name;
		Thread.currentThread().setName(name);

		if (fileLog != null) {
			return;
		}

		LOGGER_CONTEXT.start();

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
		sa.setContext(LOGGER_CONTEXT);
		sa.setName("Artemis");
		sa.setDiscriminator(discriminator);
		sa.setAppenderFactory((context, discriminatingValue) -> {
			final PatternLayoutEncoder ple = new CustomLayoutEncoder(false);
			ple.setContext(context);
			ple.setPattern(PATTERN);
			ple.setCharset(StandardCharsets.UTF_8);
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

		fileLog = LOGGER_CONTEXT.getLogger("FILE");
		fileLog.setLevel(Level.INFO);
		fileLog.setAdditive(false);
		fileLog.addAppender(sa);

		if (enableConsole) {
			final PatternLayoutEncoder ple = new CustomLayoutEncoder(true);
			ple.setContext(LOGGER_CONTEXT);
			ple.setPattern(PATTERN);
			ple.setCharset(StandardCharsets.UTF_8);
			ple.start();

			final ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
			ca.setContext(LOGGER_CONTEXT);
			ca.setEncoder(ple);
			ca.setWithJansi(true);
			ca.start();

			consoleLog = LOGGER_CONTEXT.getLogger("CONSOLE");
			consoleLog.setLevel(Level.INFO);
			consoleLog.setAdditive(false);
			consoleLog.addAppender(ca);
		}
	}

	// ---------------

	public static void info(String s, Object... params) {
		fileLog.info(s, params);

		if (doLogConsole()) {
			consoleLog.info(s, params);
		}
	}

	public static void warn(String s, Object... params) {
		fileLog.warn(s, params);

		if (doLogConsole()) {
			consoleLog.warn(s, params);
		}
	}

	public static void error(String s, Object... params) {
		fileLog.error(s, params);

		if (doLogConsole()) {
			consoleLog.error(s, params);
		}
	}

	// ------------------------------

	private static boolean doLogConsole() {
		return consoleLog != null && Thread.currentThread().getName().equals(name);
	}
}

