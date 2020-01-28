package org.continuity.api.entities.exchange;

import java.lang.reflect.Field;

public class TraceLinks extends AbstractLinks<TraceLinks> {

	private String link;

	public TraceLinks(ArtifactExchangeModel parent) {
		super(parent);
	}

	public TraceLinks() {
		this(null);
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	@Override
	public String getDefaultLink() {
		return getLink();
	}

	@Override
	public String getLink(String name) {
		return ((name == null) || name.isEmpty() || "link".equals(name)) ? link : null;
	}

	@Override
	public boolean isEmpty() {
		for (Field field : TraceLinks.class.getDeclaredFields()) {
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
	public void merge(TraceLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : TraceLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
