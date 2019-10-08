package org.continuity.api.entities.links;

import java.lang.reflect.Field;

import org.continuity.api.entities.config.LoadTestType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoadTestLinks extends AbstractLinks<LoadTestLinks> {

	@JsonProperty(value = "type", required = false)
	@JsonInclude(Include.NON_NULL)
	private LoadTestType type;

	@JsonProperty(value = "link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String link;

	@JsonProperty(value = "report-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String reportLink;

	public LoadTestLinks(LinkExchangeModel parent) {
		super(parent);
	}

	public LoadTestLinks() {
		this(null);
	}

	public LoadTestType getType() {
		return type;
	}

	public LoadTestLinks setType(LoadTestType loadTestType) {
		this.type = loadTestType;
		return this;
	}

	public String getLink() {
		return link;
	}

	public LoadTestLinks setLink(String loadTestLink) {
		this.link = loadTestLink;
		return this;
	}

	public String getReportLink() {
		return reportLink;
	}

	public LoadTestLinks setReportLink(String loadTestReportLink) {
		this.reportLink = loadTestReportLink;
		return this;
	}

	@Override
	public boolean isEmpty() {
		for (Field field : LoadTestLinks.class.getDeclaredFields()) {
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
	public void merge(LoadTestLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : LoadTestLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
