package org.continuity.idpa;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * @author Henning Schulz
 *
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public abstract class AbstractIdpaElement implements IdpaElement {

	private String id = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasId() {
		return id != null;
	}

}
