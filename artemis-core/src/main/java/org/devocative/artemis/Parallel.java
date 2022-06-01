package org.devocative.artemis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Parallel {
	private static final Logger log = LoggerFactory.getLogger(Parallel.class);

	public static Result execute(String name, int degree, Runnable runnable) {
		final Result result;

		if (degree <= 1) {
			String errStr = null;

			try {
				runnable.run();
			} catch (TestFailedException e) {
				errStr = "TestFailedException: " + e.getMessage();

				if (e.getCause() != null) {
					log.error("Parallel Execute: ", e.getCause());
				}
			} catch (Exception e) {
				log.error("Parallel Execute: ", e);
				errStr = "Exception: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getName());
			}

			result = new Result(1, errStr == null ? 0 : 1)
				.setErrors(errStr);
		} else {
			final AtomicInteger counter = new AtomicInteger(0);
			final StringBuilder builder = new StringBuilder();
			final List<Thread> list = new ArrayList<>();
			for (int i = 0; i < degree; i++) {
				final Thread t = new Thread(runnable, String.format("%s-th-%02d", name, i + 1));
				t.setUncaughtExceptionHandler((t1, e) -> {
					counter.incrementAndGet();
					synchronized (builder) {
						builder
							.append("\n")
							.append(t1.getName())
							.append(": ")
							.append(e.getMessage());
					}
				});
				t.start();
				list.add(t);
			}

			for (Thread t : list) {
				try {
					t.join();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			result = new Result(degree, counter.get())
				.setErrors(builder.toString());
		}
		return result;
	}
}
