package org.continuity.dsl.description;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents the event context type.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonDeserialize(as = FutureEvents.class)
public class FutureEvents extends Measurement implements ContextParameter {
	
	private List<FutureEvent> future;

	@JsonCreator
    public FutureEvents(@JsonProperty(value = "measurement", required = true) String measurement, @JsonProperty(value = "future", required = true) List<FutureEvent> future) {
		super(measurement);
    	this.future = future;
    }
	
	public List<FutureEvent> getFuture() {
		return future;
	}

	public void setFuture(List<FutureEvent> future) {
		this.future = future;
	}
	
	@Override
	public String toString() {
		return "FutureEvents [measurement=" + super.getMeasurement() + ", future=" + future + "]";
	}
}
