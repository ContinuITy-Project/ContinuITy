package org.continuity.api.entities.links;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LinkExchangeModel {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@JsonProperty(value = "tag", required = false)
	@JsonInclude(Include.NON_NULL)
	private String tag;

	@JsonProperty(value = "idpa", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final IdpaLinks idpaLinks = new IdpaLinks(this);

	@JsonProperty(value = "measurement-data", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final MeasurementDataLinks measurementDataLinks = new MeasurementDataLinks(this);

	@JsonProperty(value = "session-logs", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final SessionLogsLinks sessionLogsLinks = new SessionLogsLinks(this);
	
	@JsonProperty(value = "forecast", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final ForecastLinks forecastLinks = new ForecastLinks(this);

	@JsonProperty(value = "workload-model", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final WorkloadModelLinks workloadModelLinks = new WorkloadModelLinks(this);

	@JsonProperty(value = "load-test", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final LoadTestLinks loadTestLinks = new LoadTestLinks(this);
	
	@JsonProperty(value = "sessions-bundles", required = false)
	@JsonInclude(value = Include.CUSTOM, valueFilter = AbstractLinks.ValueFilter.class)
	@JsonManagedReference
	private final SessionsBundlesLinks sessionsBundlesLinks = new SessionsBundlesLinks(this);

	public String getTag() {
		return tag;
	}

	public LinkExchangeModel setTag(String tag) {
		this.tag = tag;
		return this;
	}

	public IdpaLinks getIdpaLinks() {
		return idpaLinks;
	}

	public MeasurementDataLinks getMeasurementDataLinks() {
		return measurementDataLinks;
	}

	public SessionLogsLinks getSessionLogsLinks() {
		return sessionLogsLinks;
	}
	
	public ForecastLinks getForecastLinks() {
		return forecastLinks;
	}

	public WorkloadModelLinks getWorkloadModelLinks() {
		return workloadModelLinks;
	}

	public LoadTestLinks getLoadTestLinks() {
		return loadTestLinks;
	}
	
	public SessionsBundlesLinks getSessionsBundlesLinks() {
		return sessionsBundlesLinks;
	}

	public void merge(LinkExchangeModel other) {
		if (this.getTag() == null) {
			this.setTag(other.getTag());
		}

		for (Field field : LinkExchangeModel.class.getDeclaredFields()) {
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
	private <T extends AbstractLinks<T>> void mergeLinks(Field field, LinkExchangeModel other) throws IllegalArgumentException, IllegalAccessException {
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
