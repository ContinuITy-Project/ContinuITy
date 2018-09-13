package org.continuity.api.entities.links;

import java.lang.reflect.Field;

import org.continuity.api.entities.config.WorkloadModelType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkloadModelLinks extends AbstractLinks<WorkloadModelLinks> {

	@JsonProperty(value = "type", required = false)
	@JsonInclude(Include.NON_NULL)
	private WorkloadModelType type;

	@JsonProperty(value = "link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String link;

	@JsonProperty(value = "jmeter-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String jmeterLink;
	
	@JsonProperty(value = "behavior-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String behaviorLink;

	@JsonProperty(value = "application-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String applicationLink;

	@JsonProperty(value = "initial-annotation-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String initialAnnotationLink;

	public WorkloadModelLinks(LinkExchangeModel parent) {
		super(parent);
	}

	public WorkloadModelLinks() {
		this(null);
	}

	public WorkloadModelType getType() {
		return type;
	}

	public WorkloadModelLinks setType(WorkloadModelType workloadType) {
		this.type = workloadType;
		return this;
	}

	public String getLink() {
		return link;
	}

	public WorkloadModelLinks setLink(String workloadLink) {
		this.link = workloadLink;
		return this;
	}

	public String getJmeterLink() {
		return jmeterLink;
	}

	public WorkloadModelLinks setJmeterLink(String jmeterLink) {
		this.jmeterLink = jmeterLink;
		return this;
	}
	
	public String getBehaviorLink() {
		return behaviorLink;
	}
	
	public WorkloadModelLinks setBehaviorLink(String behaviorLink) {
		this.behaviorLink = behaviorLink;
		return this;
	}

	public String getApplicationLink() {
		return applicationLink;
	}

	public WorkloadModelLinks setApplicationLink(String applicationLink) {
		this.applicationLink = applicationLink;
		return this;
	}

	public String getInitialAnnotationLink() {
		return initialAnnotationLink;
	}

	public WorkloadModelLinks setInitialAnnotationLink(String initialAnnotationLink) {
		this.initialAnnotationLink = initialAnnotationLink;
		return this;
	}

	@Override
	public boolean isEmpty() {
		for (Field field : WorkloadModelLinks.class.getDeclaredFields()) {
			try {
				if ((field.getName() != "parent") && (field.get(this) != null)) {
					return false;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	@Override
	public void merge(WorkloadModelLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : WorkloadModelLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
