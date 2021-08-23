package org.devocative.artemis;

public class TestFailedException extends RuntimeException {
	public TestFailedException(String message) {
		super(message);
	}

	public TestFailedException(String id, String message, Object... vars) {
		super(String.format("ERROR(%s) - %s", id, String.format(message, vars)));
	}
}
