package org.devocative.artemis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parallel {

	public static Map<String, Throwable> execute(int degree, Runnable runnable) {
		final Map<String, Throwable> result = new HashMap<>();
		if (degree <= 1) {
			try {
				runnable.run();
			} catch (Exception e) {
				result.put(Thread.currentThread().getName(), e);
			}
		} else {
			final List<Thread> list = new ArrayList<>();
			for (int i = 0; i < degree; i++) {
				final Thread t = new Thread(runnable);
				t.setUncaughtExceptionHandler((t1, e) -> result.put(t1.getName(), e));
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
		}
		return result;
	}

}
