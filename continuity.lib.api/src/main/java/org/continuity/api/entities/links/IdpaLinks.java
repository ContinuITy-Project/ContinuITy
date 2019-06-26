package org.continuity.api.entities.links;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IdpaLinks extends AbstractLinks<IdpaLinks> {

	@JsonProperty(value = "application-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String applicationLink;

	@JsonProperty(value = "application-delta-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String applicationDeltaLink;

	public IdpaLinks(LinkExchangeModel parent) {
		super(parent);
	}

	public IdpaLinks() {
		this(null);
	}

	public String getApplicationLink() {
		return applicationLink;
	}

	public IdpaLinks setApplicationLink(String applicationLink) {
		this.applicationLink = applicationLink;
		return this;
	}

	public String getApplicationDeltaLink() {
		return applicationDeltaLink;
	}

	public IdpaLinks setApplicationDeltaLink(String applicationDeltaLink) {
		this.applicationDeltaLink = applicationDeltaLink;
		return this;
	}

	@Override
	public boolean isEmpty() {
		for (Field field : IdpaLinks.class.getDeclaredFields()) {
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
	public void merge(IdpaLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : IdpaLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
