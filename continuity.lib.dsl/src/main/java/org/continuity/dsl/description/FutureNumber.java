package org.continuity.dsl.description;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class FutureNumber {
	
	private double value;

	@JsonProperty("time")
	@JsonSerialize(converter=FutureOccurrencesConverter.class)
	private FutureOccurrences time;
	
	@JsonCreator
    public FutureNumber(@JsonProperty(value = "value", required = true) double value, @JsonProperty(value = "time", required = true) List<String> time) {
		this.value = value;
        this.time = new FutureOccurrences(time);
    }
	
	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	@JsonIgnore
	public FutureOccurrences getTime() {
		return time;
	}

	@JsonIgnore
	public void setTime(ArrayList<String> time) {
		this.time = new FutureOccurrences(time);
	}
    @Override
	public String toString() {
		return "FutureNumber [value=" + value + ", time=" + time + "]";
	}
}
