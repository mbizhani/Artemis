package org.devocative.artemis.util;

import org.devocative.artemis.Constants;
import org.devocative.artemis.Result;
import org.devocative.artemis.TestFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
			final List<String> builder = Collections.synchronizedList(new ArrayList<>());
			final List<Thread> list = new ArrayList<>();
			for (int i = 0; i < degree; i++) {
				final Thread t = new Thread(runnable, String.format("%s" + Constants.THREAD_MIDIX + "%05d", name, i));
				t.setUncaughtExceptionHandler((t1, e) -> {
					builder.add(String.format("\n%s: %s", t1.getName(), e.getMessage()));
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

			Collections.sort(builder);

			result = new Result(degree, builder.size())
				.setErrors(String.join("", builder));
		}
		return result;
	}
}
