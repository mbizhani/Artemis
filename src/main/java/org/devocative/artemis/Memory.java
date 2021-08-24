package org.devocative.artemis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class Memory {
	public enum EStep {
		RqVars, RqCall, RqSend
	}

	@Setter(AccessLevel.NONE)
	private final List<EStep> steps = new ArrayList<>();

	private String scenarioName;
	private String rqId;
	private Context context;

	// ------------------------------

	public Memory addStep(EStep step) {
		steps.add(step);
		return this;
	}

	public Memory clear() {
		rqId = null;
		steps.clear();

		return this;
	}

	@JsonIgnore
	public boolean isEmpty() {
		return scenarioName == null && context == null;
	}
}
