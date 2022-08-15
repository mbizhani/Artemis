package org.devocative.artemis;

import lombok.RequiredArgsConstructor;

import java.util.*;

import static java.lang.Math.max;

public class StatisticsContext {
	private static final List<ExecRecord> EXEC_RECORDS = Collections.synchronizedList(new ArrayList<>());
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

	public static void execFinished(Integer iteration, long duration, String error) {
		EXEC_RECORDS.add(new ExecRecord(Thread.currentThread().getName(), iteration, duration, error));
	}

	public static void printAll() {
		if (ALL_LISTS.size() > 1) {
			ALog.info("%green(/‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾[ STATISTICS ]‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾\\)");
			printAllList();
			ALog.info("%green(\\____________________________________________/)");
		}
		ALL_LISTS.clear();
	}

	public static void printThis(long duration) {
		ALog.info("%cyan(/‾‾‾‾‾‾‾[ One-Time Execution ]‾‾‾‾‾‾‾\\)");
		printOne(CTX.get());
		ALog.info(String.format("%%cyan(\\_______[ Duration: %s ]_______/)", readableDuration(duration)));
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
		list.forEach(r -> t.addRow(r.id, r.uri, r.method, String.valueOf(r.status), readableDuration(r.duration)));
		t.print();
	}

	private static void printAllList() {
		Collections.sort(EXEC_RECORDS);
		final Tabular execStat = new Tabular("Th", "It", "Duration", "Error");
		EXEC_RECORDS.forEach(r -> execStat.addRow(r.thread, String.valueOf(r.iteration), readableDuration(r.duration), r.error));
		execStat.print();

		final Map<String, StatRecord> map = new LinkedHashMap<>();
		for (RecordList allList : ALL_LISTS) {
			for (final Record r : allList) {
				final String key = r.id + r.status;

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

		final Tabular stepsStat = new Tabular("ID", "Status", "Avg", "Count", "Min", "Min(th)", "Max", "Max(th)");
		map.values().forEach(sr ->
			stepsStat.addRow(
				sr.id,
				String.valueOf(sr.status),
				readableDuration((long) (sr.durSum / sr.count)),
				String.valueOf(sr.count),
				readableDuration(sr.min),
				String.format("[%s]", sr.minName),
				readableDuration(sr.max),
				String.format("[%s]", sr.maxName)
			));
		stepsStat.print();
	}

	public static String readableDuration(long duration) {
		final String result;

		if (duration < 1000) {
			result = String.format("%5d ms", duration);
		} else if (duration < 60_000) {
			result = String.format("%4.1f sec", duration / 1000.0);
		} else {
			result = String.format("%4.1f min", duration / 60_000.0);
		}

		return result;
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

	@RequiredArgsConstructor
	private static class ExecRecord implements Comparable<ExecRecord> {
		private final String thread;
		private final Integer iteration;
		private final long duration;
		private final String error;

		@Override
		public int compareTo(ExecRecord that) {
			return thread.compareTo(that.thread);
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
