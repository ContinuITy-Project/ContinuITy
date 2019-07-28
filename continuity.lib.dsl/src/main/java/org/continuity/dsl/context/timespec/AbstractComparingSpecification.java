package org.continuity.dsl.context.timespec;

import java.util.Date;
import java.util.List;

import org.continuity.dsl.context.TimeSpecification;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "what", "than" })
public abstract class AbstractComparingSpecification implements TimeSpecification {

	private String what;

	private double than;

	public String getWhat() {
		return what;
	}

	public void setWhat(String what) {
		this.what = what;
	}

	public double getThan() {
		return than;
	}

	public void setThan(double than) {
		this.than = than;
	}

	@Override
	public boolean appliesToDate(Date date) {
		return true;
	}

	@Override
	public boolean appliesToBoolean(List<String> occurring) {
		return true;
	}

	@Override
	public boolean appliesToString(String variable, String value) {
		return true;
	}

	@Override
	public boolean negateElasticQuery() {
		return false;
	}

}
