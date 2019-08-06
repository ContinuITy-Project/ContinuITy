package org.continuity.api.entities.exchange;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type of an artifact that can be used or produced by ContinuITy.
 *
 * @author Henning Schulz
 *
 */
public enum ArtifactType {

	/** The traces based on which everything is generated. */
	TRACES {
		@Override
		public boolean isPresentInModel(ArtifactExchangeModel model) {
			return !model.getTraceLinks().isEmpty();
		}
	},

	/** The sessions extracted from the traces. */
	SESSIONS {
		@Override
		public boolean isPresentInModel(ArtifactExchangeModel model) {
			return !model.getSessionLinks().isEmpty();
		}
	},

	/** The behavior model, e.g., Markov chains. */
	BEHAVIOR_MODEL {
		@Override
		public boolean isPresentInModel(ArtifactExchangeModel model) {
			return !model.getBehaviorModelLinks().isEmpty();
		}
	},

	/** The workload model transformed from the behavior model, including tailoring. */
	WORKLOAD_MODEL {
		@Override
		public boolean isPresentInModel(ArtifactExchangeModel model) {
			return !model.getWorkloadModelLinks().isEmpty();
		}
	},

	/** The parameterized load test. */
	LOAD_TEST {
		@Override
		public boolean isPresentInModel(ArtifactExchangeModel model) {
			return !model.getLoadTestLinks().isEmpty();
		}
	},

	/** The results of the load test execution. */
	TEST_RESULT {
		@Override
		public boolean isPresentInModel(ArtifactExchangeModel model) {
			return !model.getResultLinks().isEmpty();
		}
	};

	private static final Map<String, ArtifactType> prettyStringToType = new HashMap<>();

	static {
		for (ArtifactType type : values()) {
			prettyStringToType.put(type.toPrettyString(), type);
		}
	}

	@JsonCreator
	public static ArtifactType fromPrettyString(String key) {
		return prettyStringToType.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}

	public abstract boolean isPresentInModel(ArtifactExchangeModel model);

}
