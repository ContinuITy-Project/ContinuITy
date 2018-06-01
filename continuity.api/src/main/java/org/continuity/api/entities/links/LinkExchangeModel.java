package org.continuity.api.entities.links;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LinkExchangeModel {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@JsonProperty(value = "tag", required = false)
	@JsonInclude(Include.NON_NULL)
	private String tag;

	@JsonProperty(value = "application-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String applicationLink;

	@JsonProperty(value = "delta-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String deltaLink;

	@JsonProperty(value = "initial-annotation-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String initialAnnotationLink;

	@JsonProperty(value = "workload-type", required = false)
	@JsonInclude(Include.NON_NULL)
	private String workloadType;

	@JsonProperty(value = "workload-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String workloadLink;

	@JsonProperty(value = "jmeter-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String jmeterLink;

	@JsonProperty(value = "error", required = false)
	@JsonInclude(Include.NON_NULL)
	private Boolean error;

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getApplicationLink() {
		return applicationLink;
	}

	public void setApplicationLink(String applicationLink) {
		this.applicationLink = applicationLink;
	}

	public String getDeltaLink() {
		return deltaLink;
	}

	public void setDeltaLink(String deltaLink) {
		this.deltaLink = deltaLink;
	}

	public String getWorkloadType() {
		return workloadType;
	}

	public void setWorkloadType(String workloadType) {
		this.workloadType = workloadType;
	}

	public String getWorkloadLink() {
		return workloadLink;
	}

	public void setWorkloadLink(String workloadLink) {
		this.workloadLink = workloadLink;
	}

	public String getJmeterLink() {
		return jmeterLink;
	}

	public void setJmeterLink(String jmeterLink) {
		this.jmeterLink = jmeterLink;
	}

	public String getInitialAnnotationLink() {
		return initialAnnotationLink;
	}

	public void setInitialAnnotationLink(String initialAnnotationLink) {
		this.initialAnnotationLink = initialAnnotationLink;
	}

	public Boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
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
