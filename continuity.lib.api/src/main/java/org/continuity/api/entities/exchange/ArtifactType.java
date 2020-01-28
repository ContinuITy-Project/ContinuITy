package org.continuity.api.entities.exchange;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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
	TRACES(ArtifactExchangeModel::getTraceLinks),

	/** The sessions extracted from the traces. */
	SESSIONS(ArtifactExchangeModel::getSessionLinks),

	/** The behavior model, e.g., Markov chains. */
	BEHAVIOR_MODEL(ArtifactExchangeModel::getBehaviorModelLinks),

	/** The workload model transformed from the behavior model, including tailoring. */
	WORKLOAD_MODEL(ArtifactExchangeModel::getWorkloadModelLinks),

	/** The parameterized load test. */
	LOAD_TEST(ArtifactExchangeModel::getLoadTestLinks),

	/** The results of the load test execution. */
	TEST_RESULT(ArtifactExchangeModel::getResultLinks),

	/** A link to the intensity. */
	INTENSITY(null) {

		@Override
		public boolean isPresentInModel(ArtifactExchangeModel model) {
			return model.getIntensity() != null;
		}

		@Override
		public AbstractLinks<?> getFromModel(ArtifactExchangeModel model) {
			return new SingleLink(model, model.getIntensity());
		}

	};

	private static final Map<String, ArtifactType> prettyStringToType = new HashMap<>();

	private final Function<ArtifactExchangeModel, AbstractLinks<?>> getter;

	static {
		for (ArtifactType type : values()) {
			prettyStringToType.put(type.toPrettyString(), type);
		}
	}

	private ArtifactType(Function<ArtifactExchangeModel, AbstractLinks<?>> getter) {
		this.getter = getter;
	}

	@JsonCreator
	public static ArtifactType fromPrettyString(String key) {
		return prettyStringToType.get(key);
	}

	@JsonValue
	public String toPrettyString() {
		return toString().replace("_", "-").toLowerCase();
	}

	public boolean isPresentInModel(ArtifactExchangeModel model) {
		return !getter.apply(model).isEmpty();
	}

	public AbstractLinks<?> getFromModel(ArtifactExchangeModel model) {
		return getter.apply(model);
	}

	private static class SingleLink extends AbstractLinks<SingleLink> {

		private String link;

		public SingleLink(ArtifactExchangeModel parent, String link) {
			super(parent);
			this.link = link;
		}

		@Override
		public boolean isEmpty() {
			return link == null;
		}

		@Override
		public void merge(SingleLink other) throws IllegalArgumentException, IllegalAccessException {
			if ((other != null) && (this.link == null)) {
				this.link = other.link;
			}
		}

		@Override
		public String getDefaultLink() {
			return link;
		}

		@Override
		public String getLink(String name) {
			return ((name == null) || name.isEmpty() || "link".equals(name)) ? link : null;
		}

	}

}
