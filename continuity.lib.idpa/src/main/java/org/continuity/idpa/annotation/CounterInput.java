package org.continuity.idpa.annotation;

import java.text.DecimalFormat;

import org.continuity.idpa.AbstractIdpaElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a counter that can be used as an input. Defines a range, increment, format (as
 * {@link DecimalFormat}) and a {@link Scope}.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "format", "scope", "start", "increment", "maximum" })
public class CounterInput extends AbstractIdpaElement implements Input {

	@JsonInclude(Include.NON_NULL)
	private String format;

	private Scope scope;

	private long start;

	private long increment;

	private long maximum;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Scope getScope() {
		return scope;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getIncrement() {
		return increment;
	}

	public void setIncrement(long increment) {
		this.increment = increment;
	}

	public long getMaximum() {
		return maximum;
	}

	public void setMaximum(long maximum) {
		this.maximum = maximum;
	}

	/**
	 * Defines the scope within which a counter is valid. Can be {@link #GLOBAL}, {@link #USER} or
	 * {@link #USER_ITERATION}.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static enum Scope {
		/**
		 * The counter is shared between all users.
		 */
		GLOBAL,

		/**
		 * Each user has an individual instance of the counter.
		 */
		USER,

		/**
		 * Each user has an individual instance of the counter and the counter is reset after each
		 * iteration.
		 */
		USER_ITERATION
	}

}
