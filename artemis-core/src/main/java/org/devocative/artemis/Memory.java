package org.devocative.artemis;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class Memory {
	public enum EStep {
		RqVars, RqCall, RqSend
	}

	private final List<EStep> steps = new ArrayList<>();

	private String scenarioName;
	private String rqId;
	private String lastSuccessfulRqId;
	private Context context;

	// ------------------------------

	List<EStep> getSteps() {
		return steps;
	}

	String getScenarioName() {
		return scenarioName;
	}

	void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
	}

	String getRqId() {
		return rqId;
	}

	void setRqId(String rqId) {
		this.rqId = rqId;
	}

	String getLastSuccessfulRqId() {
		return lastSuccessfulRqId;
	}

	void setLastSuccessfulRqId(String lastSuccessfulRqId) {
		this.lastSuccessfulRqId = lastSuccessfulRqId;
	}

	Context getContext() {
		return context;
	}

	void setContext(Context context) {
		this.context = context;
	}

	// ---------------

	void addStep(EStep step) {
		steps.add(step);
	}

	void clear() {
		if (rqId != null) {
			lastSuccessfulRqId = rqId;
			rqId = null;
		}
		steps.clear();
	}

	void clearAll() {
		lastSuccessfulRqId = null;
		rqId = null;
		steps.clear();
	}

	@JsonIgnore
	boolean isEmpty() {
		return scenarioName == null && context == null;
	}
}
