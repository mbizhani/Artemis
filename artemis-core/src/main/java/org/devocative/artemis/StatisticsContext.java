package org.devocative.artemis;

import lombok.RequiredArgsConstructor;

import java.util.*;

import static java.lang.Math.max;

public class StatisticsContext {
	private static final List<RecordList> ALL_LISTS = Collections.synchronizedList(new ArrayList<>());
	private static final ThreadLocal<RecordList> CTX = new ThreadLocal<>();

	// ------------------------------

	public static void add(String id, String method, String uri, int status, long duration) {
		if (CTX.get() == null) {
			CTX.set(createList());
		}
		CTX.get()
			.add(new Record(id, method, uri, status, duration, Thread.currentThread().getName()));
	}

	public static void print() {
		if (ALL_LISTS.size() > 1) {
			printAll();
		}
		ALL_LISTS.clear();
	}

	public static void printThis() {
		printOne(CTX.get());
		CTX.remove();
	}

	// ------------------------------

	private synchronized static RecordList createList() {
		final RecordList list = new RecordList();
		ALL_LISTS.add(list);
		return list;
	}

	private static void printOne(RecordList list) {
		final Tabular t = new Tabular("ID", "URI", "Method", "Status", "Duration");
		list.forEach(r -> t.addRow(r.id, r.uri, r.method, String.valueOf(r.status), String.valueOf(r.duration)));
		t.print();
	}

	private static void printAll() {
		final Map<String, StatRecord> map = new LinkedHashMap<>();
		for (int i = 0; i < ALL_LISTS.size(); i++) {
			for (final Record r : ALL_LISTS.get(i)) {
				final String key = r.id + r.status;
				if (i == 0) {
					map.put(key, new StatRecord(r));
				} else {
					if (map.containsKey(key)) {
						final StatRecord sr = map.get(key);
						sr.count++;
						sr.durSum += r.duration;
						if (sr.min > r.duration) {
							sr.min = r.duration;
							sr.minName = r.threadName;
						}
						if (sr.max < r.duration) {
							sr.max = r.duration;
							sr.maxName = r.threadName;
						}
					} else {
						map.put(key, new StatRecord(r));
					}
				}
			}
		}

		final Tabular t = new Tabular("ID", "Status", "Avg", "Count", "Min", "Min(th)", "Max", "Max(th)");
		map.values().forEach(sr ->
			t.addRow(
				sr.id,
				String.valueOf(sr.status),
				String.format("%.2f", sr.durSum / sr.count),
				String.valueOf(sr.count),
				String.valueOf(sr.min),
				String.format("[%s]", sr.minName),
				String.valueOf(sr.max),
				String.format("[%s]", sr.maxName)
			));
		t.print();
	}

	// ------------------------------

	@RequiredArgsConstructor
	private static class Record {
		private final String id;
		private final String method;
		private final String uri;
		private final int status;
		private final long duration;
		private final String threadName;
	}

	private static class StatRecord {
		private final String id;
		private final int status;
		private double durSum;
		private int count = 1;
		private long max;
		private String maxName;
		private long min;
		private String minName;

		public StatRecord(Record r) {
			id = r.id;
			status = r.status;
			durSum = r.duration;
			max = r.duration;
			min = r.duration;
			maxName = r.threadName;
			minName = r.threadName;
		}
	}

	private static class RecordList extends ArrayList<Record> {
	}

	private static class Tabular {
		private final String[] header;
		private final Integer[] width;
		private final List<String[]> rows = new ArrayList<>();

		// ------------------------------

		public Tabular(String... header) {
			this.header = header;

			width = new Integer[header.length];
			for (int i = 0; i < header.length; i++) {
				width[i] = header[i].length();
			}
		}

		// ------------------------------

		public void addRow(String... cells) {
			final String[] row = new String[header.length];

			if (cells.length == header.length) {
				for (int i = 0; i < cells.length; i++) {
					final String cell = cells[i] != null ? cells[i] : "";
					width[i] = max(width[i], cell.length());
					row[i] = cell;
				}
			} else {
				throw new RuntimeException("Invalid row size: " + Arrays.toString(cells));
			}

			rows.add(row);
		}

		public void print() {
			final StringBuilder builder = new StringBuilder();

			for (int i = 0; i < width.length - 1; i++) {
				builder.append("%-").append(width[i]).append("s  ");
			}
			builder.append("%s");

			final String format = builder.toString();
			ALog.info(String.format(format, (Object[]) header));

			for (String[] row : rows) {
				ALog.info(String.format(format, (Object[]) row));
			}
		}
	}
}
