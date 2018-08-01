package org.continuity.api.entities.links;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AbstractLinks<T extends AbstractLinks<T>> {

	@JsonBackReference
	private final LinkExchangeModel parent;

	public AbstractLinks(LinkExchangeModel parent) {
		this.parent = parent;
	}

	public LinkExchangeModel parent() {
		return parent;
	}

	@JsonIgnore
	public abstract boolean isEmpty();

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
