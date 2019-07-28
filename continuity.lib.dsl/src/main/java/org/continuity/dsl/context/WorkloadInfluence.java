package org.continuity.dsl.context;

import org.continuity.dsl.context.influence.DecreasedInfluence;
import org.continuity.dsl.context.influence.FixedInfluence;
import org.continuity.dsl.context.influence.IncreasedInfluence;
import org.continuity.dsl.context.influence.IsAbsentInfluence;
import org.continuity.dsl.context.influence.MultipliedInfluence;
import org.continuity.dsl.context.influence.OccursInfluence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Specifies variables that influence the workload and need to be considered for generating the
 * workload model.
 *
 * @author Henning Schulz
 *
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY)
@JsonSubTypes({ @Type(value = FixedInfluence.class, name = "fixed"), @Type(value = OccursInfluence.class, name = "occurs"), @Type(value = IsAbsentInfluence.class, name = "is-absent"),
		@Type(value = MultipliedInfluence.class, name = "multiplied"),
		@Type(value = IncreasedInfluence.class, name = "increased"), @Type(value = DecreasedInfluence.class, name = "decreased") })
public interface WorkloadInfluence {

}
