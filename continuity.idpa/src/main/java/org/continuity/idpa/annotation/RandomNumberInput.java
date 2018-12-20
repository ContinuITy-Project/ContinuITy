package org.continuity.idpa.annotation;

import java.util.Objects;

import org.continuity.idpa.AbstractIdpaElement;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * Generates random numbers based on lower and upper limits.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "lower", "lower-input", "upper", "upper-input" })
public class RandomNumberInput extends AbstractIdpaElement implements Input {

	private static final String MAX_VAL = "2147483647";

	private static final String MIN_VAL = "-2147483648";

	@JsonProperty(value = "lower", defaultValue = MAX_VAL)
	@JsonInclude(value = Include.CUSTOM, valueFilter = MaxValFilter.class)
	private int staticLowerLimit = Integer.MAX_VALUE;

	@JsonProperty(value = "upper", defaultValue = MIN_VAL)
	@JsonInclude(value = Include.CUSTOM, valueFilter = MinValFilter.class)
	private int staticUpperLimit = Integer.MIN_VALUE;

	@JsonProperty(value = "lower-input", required = false)
	@JsonInclude(Include.NON_NULL)
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	private Input derivedLowerLimit;

	@JsonProperty(value = "upper-input", required = false)
	@JsonInclude(Include.NON_NULL)
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	private Input derivedUpperLimit;

	public int getStaticLowerLimit() {
		return staticLowerLimit;
	}

	public void setStaticLowerLimit(int staticLowerLimit) {
		this.staticLowerLimit = staticLowerLimit;
	}

	public int getStaticUpperLimit() {
		return staticUpperLimit;
	}

	public void setStaticUpperLimit(int staticUpperLimit) {
		this.staticUpperLimit = staticUpperLimit;
	}

	public Input getDerivedLowerLimit() {
		return derivedLowerLimit;
	}

	public void setDerivedLowerLimit(Input derivedLowerLimit) {
		this.derivedLowerLimit = derivedLowerLimit;
	}

	public Input getDerivedUpperLimit() {
		return derivedUpperLimit;
	}

	public void setDerivedUpperLimit(Input derivedUpperLimit) {
		this.derivedUpperLimit = derivedUpperLimit;
	}

	@JsonIgnore
	public boolean lowerIsStatic() {
		return derivedLowerLimit == null;
	}

	@JsonIgnore
	public boolean upperIsStatic() {
		return derivedUpperLimit == null;
	}

	private static class MaxValFilter {

		@Override
		public boolean equals(Object obj) {
			return MAX_VAL.equals(Objects.toString(obj));
		}

	}

	private static class MinValFilter {

		@Override
		public boolean equals(Object obj) {
			return MIN_VAL.equals(Objects.toString(obj));
		}

	}

}
