package org.continuity.api.entities.links;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionsBundlesLinks extends AbstractLinks<SessionsBundlesLinks> {

	@JsonProperty(value = "link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String link;
	
	@JsonProperty(value = "status", required = false)
	@JsonInclude(Include.NON_NULL)
	private SessionsStatus status = SessionsStatus.CHANGED;

	public SessionsBundlesLinks(LinkExchangeModel parent) {
		super(parent);
	}

	public SessionsBundlesLinks() {
		this(null);
	}

	public String getLink() {
		return link;
	}

	public SessionsBundlesLinks setLink(String sessionsBundlesLink) {
		this.link = sessionsBundlesLink;
		return this;
	}
	
	public SessionsStatus getStatus() {
		return status;
	}

	public void setStatus(SessionsStatus status) {
		this.status = status;
	}

	@Override
	public boolean isEmpty() {
		for (Field field : SessionsBundlesLinks.class.getDeclaredFields()) {
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
	public void merge(SessionsBundlesLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : SessionsBundlesLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
