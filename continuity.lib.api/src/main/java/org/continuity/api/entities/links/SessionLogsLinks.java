package org.continuity.api.entities.links;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionLogsLinks extends AbstractLinks<SessionLogsLinks> {

	@JsonProperty(value = "simple-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String simpleLink;

	@JsonProperty(value = "extended-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String extendedLink;

	public SessionLogsLinks(LinkExchangeModel parent) {
		super(parent);
	}

	public SessionLogsLinks() {
		this(null);
	}

	public String getSimpleLink() {
		return simpleLink;
	}

	public SessionLogsLinks setSimpleLink(String simpleLink) {
		this.simpleLink = simpleLink;
		return this;
	}

	public String getExtendedLink() {
		return extendedLink;
	}

	public SessionLogsLinks setExtendedLink(String extendedLink) {
		this.extendedLink = extendedLink;
		return this;
	}


	@Override
	public boolean isEmpty() {
		for (Field field : SessionLogsLinks.class.getDeclaredFields()) {
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
	public void merge(SessionLogsLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : SessionLogsLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
