package org.continuity.dsl.adjustment;

import java.util.Optional;

import org.continuity.dsl.context.WorkloadAdjustment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Decreases the intensity either of all groups or of a selected one by an amount.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "by", "group" })
public class IntensityDecreasedAdjustment implements WorkloadAdjustment {

	private double by;

	@JsonInclude(Include.NON_ABSENT)
	private Optional<String> group;

	public double getBy() {
		return by;
	}

	public void setBy(double by) {
		this.by = by;
	}

	public Optional<String> getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = Optional.of(group);
	}

}
