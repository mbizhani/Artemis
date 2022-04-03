package org.devocative.artemis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Parallel {

	public static Result execute(String name, int degree, Runnable runnable) {
		final Result result;

		if (degree <= 1) {
			String errStr = null;

			try {
				runnable.run();
			} catch (Exception e) {
				errStr = e.getMessage();
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
