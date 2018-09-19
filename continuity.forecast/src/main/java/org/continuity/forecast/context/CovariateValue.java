package org.continuity.forecast.context;

import org.continuity.forecast.context.deserializer.CovariateDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = CovariateDeserializer.class)
public interface CovariateValue {

}
