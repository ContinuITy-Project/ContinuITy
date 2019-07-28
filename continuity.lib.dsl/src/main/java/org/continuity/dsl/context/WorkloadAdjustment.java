package org.continuity.dsl.context;

import org.continuity.dsl.adjustment.IntensityDecreasedAdjustment;
import org.continuity.dsl.adjustment.IntensityIncreasedAdjustment;
import org.continuity.dsl.adjustment.IntensityMultipliedAdjustment;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Specifies an adjustment of the generated workload model.
 *
 * @author Henning Schulz
 *
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY)
@JsonSubTypes({ @Type(value = IntensityMultipliedAdjustment.class, name = "intensity-multiplied"), @Type(value = IntensityIncreasedAdjustment.class, name = "intensity-increased"),
		@Type(value = IntensityDecreasedAdjustment.class, name = "intensity-descreased") })
public interface WorkloadAdjustment {

}
