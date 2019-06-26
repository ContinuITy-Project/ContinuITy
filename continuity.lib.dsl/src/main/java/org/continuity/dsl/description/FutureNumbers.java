package org.continuity.dsl.description;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents the continuous data context type.
 * @author Alper Hidiroglu
 *
 */
@JsonDeserialize(as = FutureNumbers.class)
public class FutureNumbers extends Measurement implements ContextParameter {
	
	private List<FutureNumber> future;

	@JsonCreator
    public FutureNumbers(@JsonProperty(value = "measurement", required = true) String measurement, @JsonProperty(value = "future", required = true) List<FutureNumber> future) {
		super(measurement);
    	this.future = future;
    }
	
	public List<FutureNumber> getFuture() {
		return future;
	}

	public void setFuture(List<FutureNumber> future) {
		this.future = future;
	}
	
	@Override
	public String toString() {
		return "FutureNumbers [measurement=" + super.getMeasurement() + ", future=" + future + "]";
	}

}
