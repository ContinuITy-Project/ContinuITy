package org.continuity.api.entities.exchange;

import java.lang.reflect.Field;

import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ArtifactExchangeModel {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@JsonProperty(value = "app-id", required = false)
	@JsonInclude(Include.NON_NULL)
	private AppId appId;

	@JsonProperty(value = "version", required = false)
	@JsonInclude(Include.NON_NULL)
	private VersionOrTimestamp version;

	@JsonProperty(value = "traces", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final TraceLinks traceLinks = new TraceLinks(this);

	@JsonProperty(value = "sessions", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final SessionLinks sessionLinks = new SessionLinks(this);

	@JsonProperty(value = "behavior-model", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final BehaviorModelLinks behaviorModelLinks = new BehaviorModelLinks(this);

	@JsonProperty(value = "workload-model", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final WorkloadModelLinks workloadModelLinks = new WorkloadModelLinks(this);

	@JsonProperty(value = "load-test", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final LoadTestLinks loadTestLinks = new LoadTestLinks(this);

	@JsonProperty(value = "test-result", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final LoadTestResultLinks resultLinks = new LoadTestResultLinks(this);

	public AppId getAppId() {
		return appId;
	}

	public ArtifactExchangeModel setAppId(AppId appId) {
		this.appId = appId;
		return this;
	}

	public VersionOrTimestamp getVersion() {
		return version;
	}

	public ArtifactExchangeModel setVersion(VersionOrTimestamp version) {
		this.version = version;
		return this;
	}

	public TraceLinks getTraceLinks() {
		return traceLinks;
	}

	public SessionLinks getSessionLinks() {
		return sessionLinks;
	}

	public BehaviorModelLinks getBehaviorModelLinks() {
		return behaviorModelLinks;
	}

	public WorkloadModelLinks getWorkloadModelLinks() {
		return workloadModelLinks;
	}

	public LoadTestLinks getLoadTestLinks() {
		return loadTestLinks;
	}

	public LoadTestResultLinks getResultLinks() {
		return resultLinks;
	}

	@JsonIgnore
	public boolean isPresent(ArtifactType type) {
		return type.isPresentInModel(this);
	}

	public void merge(ArtifactExchangeModel other) {
		if (this.getAppId() == null) {
			this.setAppId(other.getAppId());
		}

		for (Field field : ArtifactExchangeModel.class.getDeclaredFields()) {
			if (AbstractLinks.class.isAssignableFrom(field.getType())) {
				try {
					mergeLinks(field, other);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends AbstractLinks<T>> void mergeLinks(Field field, ArtifactExchangeModel other) throws IllegalArgumentException, IllegalAccessException {
		((T) field.get(this)).merge((T) field.get(other));
	}

	@Override
	public String toString() {
		try {
			return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return e + " during serialization!";
		}
	}

}
