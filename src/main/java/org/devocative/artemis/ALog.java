package org.devocative.artemis;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;

public class ALog {
	private static Logger log = null;

	public synchronized static void init(String name) {
		if (log != null) {
			return;
		}

		final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

		final MDCBasedDiscriminator discriminator = new MDCBasedDiscriminator();
		discriminator.setContext(lc);
		discriminator.setKey("threadName");
		discriminator.setDefaultValue(name);
		discriminator.start();

		final SiftingAppender sa = new SiftingAppender();
		sa.setContext(lc);
		sa.setName("Artemis");
		sa.setDiscriminator(discriminator);
		sa.setAppenderFactory((context, discriminatingValue) -> {
			final PatternLayoutEncoder ple = new PatternLayoutEncoder();
			ple.setContext(context);
			ple.setPattern("%date - %msg%n");
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

		log = logger;
	}

	public static void info(String s, Object... params) {
		log.info(s, params);
	}

	public static void warn(String s, Object... params) {
		log.warn(s, params);
	}
}
