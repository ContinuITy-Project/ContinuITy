package org.continuity.dsl.context.timespec;

import java.util.Date;
import java.util.List;

import org.continuity.dsl.context.TimeSpecification;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 *
 * @author Henning Schulz
 *
 */
public abstract class AbstractDateSpecification implements TimeSpecification {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
	private Date date;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public boolean appliesToNumerical(String variable, double value) {
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
