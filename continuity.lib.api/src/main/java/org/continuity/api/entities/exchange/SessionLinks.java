package org.continuity.api.entities.exchange;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionLinks extends AbstractLinks<SessionLinks> {

	@JsonProperty(value = "simple-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String simpleLink;

	@JsonProperty(value = "extended-link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String extendedLink;

	public SessionLinks(ArtifactExchangeModel parent) {
		super(parent);
	}

	public SessionLinks() {
		this(null);
	}

	public String getSimpleLink() {
		return simpleLink;
	}

	public SessionLinks setSimpleLink(String simpleLink) {
		this.simpleLink = simpleLink;
		return this;
	}

	public String getExtendedLink() {
		return extendedLink;
	}

	public SessionLinks setExtendedLink(String extendedLink) {
		this.extendedLink = extendedLink;
		return this;
	}

	@Override
	public String getDefaultLink() {
		return getSimpleLink();
	}

	@Override
	public String getLink(String name) {
		switch (name) {
		case "extended-link":
		case "extended":
			return getExtendedLink();
		case "simple-link":
		case "simple":
			return getDefaultLink();
		default:
			return null;
		}
	}

	@Override
	public boolean isEmpty() {
		for (Field field : SessionLinks.class.getDeclaredFields()) {
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
	public void merge(SessionLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : SessionLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
