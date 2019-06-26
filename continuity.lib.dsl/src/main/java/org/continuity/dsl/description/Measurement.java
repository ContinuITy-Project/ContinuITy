package org.continuity.dsl.description;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = Measurement.class)
public class Measurement implements ContextParameter {
	
	@JsonProperty("measurement")
	private String measurement;

	@JsonCreator
    public Measurement(@JsonProperty(value = "measurement", required = true) String measurement) {
    	this.measurement = measurement;
    }
	
	public String getMeasurement() {
		return measurement;
	}

	public void setMeasurement(String measurement) {
		this.measurement = measurement;
	}
	
	@Override
	public String toString() {
		return "Measurement [measurement=" + measurement + "]";
	}
}
