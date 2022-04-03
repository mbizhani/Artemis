package org.devocative.artemis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
public class Result {
	private final int degree;
	private final int noOfErrors;
	private String errors;

	public boolean hasError() {
		return noOfErrors > 0;
	}
}
