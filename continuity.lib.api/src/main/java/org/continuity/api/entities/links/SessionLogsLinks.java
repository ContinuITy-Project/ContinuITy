package org.continuity.api.entities.links;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionLogsLinks extends AbstractLinks<SessionLogsLinks> {

	@JsonProperty(value = "link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String link;

	public SessionLogsLinks(LinkExchangeModel parent) {
		super(parent);
	}

	public SessionLogsLinks() {
		this(null);
	}

	public String getLink() {
		return link;
	}

	public SessionLogsLinks setLink(String sessionLogsLink) {
		this.link = sessionLogsLink;
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
