package org.continuity.dsl.description;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class FutureEvent {
	
	private String value;

	@JsonProperty("time")
	@JsonSerialize(converter=FutureOccurrencesConverter.class)
	private FutureOccurrences time;

	@JsonCreator
    public FutureEvent(@JsonProperty(value = "value", required = true) String value, @JsonProperty(value = "time", required = true) List<String> time) {
    	this.value = value;
        this.time = new FutureOccurrences(time);
    }
    
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
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
		return "FutureEvent [value=" + value + ", time=" + time + "]";
	}
}
