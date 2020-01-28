package org.continuity.api.entities.exchange;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AbstractLinks<T extends AbstractLinks<T>> {

	@JsonBackReference
	private final ArtifactExchangeModel parent;

	public AbstractLinks(ArtifactExchangeModel parent) {
		this.parent = parent;
	}

	public ArtifactExchangeModel parent() {
		return parent;
	}

	@JsonIgnore
	public abstract boolean isEmpty();

	@JsonIgnore
	public abstract String getDefaultLink();

	@JsonIgnore
	public abstract String getLink(String name);

	public abstract void merge(T other) throws IllegalArgumentException, IllegalAccessException;

	public static class ValueFilter {

		@Override
		public boolean equals(Object obj) {
			if ((obj == null) || !(obj instanceof AbstractLinks)) {
				return false;
			}

			AbstractLinks<?> links = (AbstractLinks<?>) obj;
			return links.isEmpty();
		}

	}

}
