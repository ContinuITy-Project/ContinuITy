package org.continuity.workload.dsl.annotation;

import java.util.ArrayList;
import java.util.List;

import org.continuity.workload.dsl.AbstractContinuityModelElement;

/**
 * Common superclass for annotations that can hold {@link PropertyOverride}s.
 *
 * @author Henning Schulz
 *
 * @param <A>
 *            Annotated type.
 */
public class OverrideableAnnotation<T extends PropertyOverrideKey.Any> extends AbstractContinuityModelElement {

	private List<PropertyOverride<T>> overrides;

	/**
	 * Gets the overrides.
	 *
	 * @return The overrides.
	 */
	public List<PropertyOverride<T>> getOverrides() {
		if (overrides == null) {
			overrides = new ArrayList<>();
		}

		return this.overrides;
	}

	/**
	 * Sets the overrides.
	 *
	 * @param overrides
	 *            The overrides.
	 */
	public void setOverrides(List<PropertyOverride<T>> overrides) {
		this.overrides = overrides;
	}

	/**
	 * Adds a new override.
	 *
	 * @param override
	 *            The override to be added.
	 */
	public void addOverride(PropertyOverride<T> override) {
		getOverrides().add(override);
	}

}
