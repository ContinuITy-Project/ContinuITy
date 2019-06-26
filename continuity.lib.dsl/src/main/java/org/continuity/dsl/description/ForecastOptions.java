package org.continuity.dsl.description;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Further information for the workload forecasting.
 * 
 * @author Alper Hidiroglu
 *
 */
@JsonPropertyOrder({ "forecast-date", "interval", "forecaster", "influx-link" })
public class ForecastOptions {

	@JsonProperty("forecast-date")
	@JsonSerialize(converter=ForecastDateConverter.class)
	private Date forecastDate;
	
	private IntensityCalculationInterval interval;
	
	private String forecaster;
	
	@JsonProperty("influx-link")
	private String influxLink;

	@JsonIgnore
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	@JsonCreator
    public ForecastOptions(@JsonProperty(value = "forecast-date", required = true) String forecastPeriod, @JsonProperty(value = "interval", required = true) IntensityCalculationInterval interval
    		,@JsonProperty(value = "forecaster", required = true) String forecaster, @JsonProperty(value = "influx-link", required = true) String influxLink) {
    	this.forecastDate = null;
    	try {
		    forecastDate = dateFormat.parse(forecastPeriod);
		} catch (ParseException e) {
		    e.printStackTrace();
		}
    	this.forecaster = forecaster;
    	this.interval = interval;
    	this.influxLink = influxLink;
    }

	public IntensityCalculationInterval getInterval() {
		return interval;
	}

	public void setInterval(IntensityCalculationInterval interval) {
		this.interval = interval;
	}

	/**
	 * Gets the period of the workload forecast.
	 * 
	 * @return The period of the workload forecast.
	 */
	public Date getForecastDate() {
		return forecastDate;
	}

	/**
	 * Sets the period of the workload forecasting.
	 * 
	 * @param forecastPeriod The period of the workload forecast.
	 */
	public void setForecastDate(Date forecastDate) {
		this.forecastDate = forecastDate;
	}
	
	public String getForecaster() {
		return forecaster;
	}

	public void setForecaster(String forecaster) {
		this.forecaster = forecaster;
	}
	
	@JsonIgnore
	public long getDateAsTimestamp() {
		// 13 digits
		return this.forecastDate.getTime();
	}
	

	public String getInfluxLink() {
		return influxLink;
	}

	public void setInfluxLink(String influxLink) {
		this.influxLink = influxLink;
	}

	@Override
	public String toString() {
		return "Forecast [forecast-date=" + forecastDate + ", interval=" + interval + ", forecaster=" + forecaster + "]";
	}	
}
