package org.continuity.dsl.description;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents a context input.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonPropertyOrder({ "context", "forecast-options" })
public class ForecastInput {

	private List<ContextParameter> context;

	@JsonProperty("forecast-options")
	private ForecastOptions forecastOptions;
	
	@JsonCreator
    public ForecastInput(@JsonProperty(value = "context", required = false) List<ContextParameter> context, 
    		@JsonProperty(value = "forecast-options", required = true) ForecastOptions forecastOptions) {
    	this.context = context;
    	this.forecastOptions = forecastOptions;
    }

	/**
	 * Returns context for the workload forecasting.
	 * 
	 * @return The context covariates.
	 */
	public List<ContextParameter> getContext() {
		return context;
	}

	/**
	 * Sets the context for the workload forecasting.
	 * 
	 * @param context The context covariates.
	 * 
	 */
	public void setContext(List<ContextParameter> context) {
		this.context = context;
	}

	/**
	 * Gets further information for the workload forecasting.
	 * 
	 * @return The forecasting information.
	 */
	public ForecastOptions getForecastOptions() {
		return forecastOptions;
	}

	/**
	 * Sets further information for the workload forecasting.
	 * 
	 * @param forecast The forecasting information.
	 */
	public void setForecastOptions(ForecastOptions forecast) {
		this.forecastOptions = forecast;
	}

	@Override
	public String toString() {
		return "Forecast-Input [context=" + context + ", forecastOptions=" + forecastOptions + "]";
	}

}
