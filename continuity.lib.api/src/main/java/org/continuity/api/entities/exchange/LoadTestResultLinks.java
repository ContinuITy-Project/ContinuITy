package org.continuity.api.entities.exchange;

import java.lang.reflect.Field;

/**
 *
 * @author Henning Schulz
 *
 */
public class LoadTestResultLinks extends AbstractLinks<LoadTestResultLinks> {

	private String link;

	public LoadTestResultLinks(ArtifactExchangeModel parent) {
		super(parent);
	}

	public LoadTestResultLinks() {
		this(null);
	}

	public String getLink() {
		return link;
	}

	public LoadTestResultLinks setLink(String link) {
		this.link = link;
		return this;
	}

	@Override
	public boolean isEmpty() {
		for (Field field : LoadTestResultLinks.class.getDeclaredFields()) {
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
	public void merge(LoadTestResultLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : LoadTestResultLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
