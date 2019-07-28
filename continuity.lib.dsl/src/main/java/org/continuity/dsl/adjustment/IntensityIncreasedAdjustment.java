package org.continuity.dsl.adjustment;

import java.util.Optional;

import org.continuity.dsl.context.WorkloadAdjustment;
import org.continuity.dsl.serialize.OptionalConverter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Increases the intensity either of all groups or of a selected one by an amount.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "by", "group" })
public class IntensityIncreasedAdjustment implements WorkloadAdjustment {

	private double by;

	@JsonInclude(Include.NON_ABSENT)
	@JsonSerialize(converter = OptionalConverter.class)
	private Optional<Integer> group;

	public double getBy() {
		return by;
	}

	public void setBy(double by) {
		this.by = by;
	}

	public Optional<Integer> getGroup() {
		return group;
	}

	public void setGroup(int group) {
		this.group = Optional.of(group);
	}

}
