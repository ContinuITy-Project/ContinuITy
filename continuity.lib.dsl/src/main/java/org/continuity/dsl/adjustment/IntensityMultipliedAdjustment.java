package org.continuity.dsl.adjustment;

import java.util.Optional;

import org.continuity.dsl.context.WorkloadAdjustment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Multiplies the intensity either of all groups or of a selected one with a factor.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "with", "group" })
public class IntensityMultipliedAdjustment implements WorkloadAdjustment {

	private double with;

	@JsonInclude(Include.NON_ABSENT)
	private Optional<String> group;

	public double getWith() {
		return with;
	}

	public void setWith(double with) {
		this.with = with;
	}

	public Optional<String> getGroup() {
		return group;
	}

	public void setGroup(Optional<String> group) {
		this.group = group;
	}

}
