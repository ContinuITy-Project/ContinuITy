package org.continuity.dsl.context.influence;

import java.util.List;

import org.continuity.dsl.context.TimeSpecification;
import org.continuity.dsl.context.WorkloadInfluence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public abstract class AbstractWorkloadInfluence implements WorkloadInfluence {

	@JsonInclude(Include.NON_EMPTY)
	private List<TimeSpecification> when;

	public List<TimeSpecification> getWhen() {
		return when;
	}

	public void setWhen(List<TimeSpecification> when) {
		this.when = when;
	}

}
