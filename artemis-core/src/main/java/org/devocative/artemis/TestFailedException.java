package org.devocative.artemis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class TestFailedException extends RuntimeException {
	private Integer degree;
	private Integer noOfErrors;

	public TestFailedException(String message) {
		super(message);
	}

	public TestFailedException(String id, String message, Object... vars) {
		super(String.format("ERROR(%s) - %s", id, String.format(message, vars)));
	}
}
