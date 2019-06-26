package org.continuity.forecast.managers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.continuity.forecast.context.BooleanCovariateValue;
import org.continuity.forecast.context.CovariateData;
import org.continuity.forecast.context.CovariateValue;
import org.continuity.forecast.context.NumericalCovariateValue;
import org.continuity.forecast.context.StringCovariateValue;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;

/**
 * Manages covariate data.
 * @author Alper Hidiroglu
 *
 */
public class CovariateDataManager {
	
	private InfluxDB influxDb;
	
	/**
	 * @param influxDb
	 */
	public CovariateDataManager(InfluxDB influxDb) {
		this.influxDb = influxDb;
	}
	
	/**
	 * @param covarData
	 */
	@SuppressWarnings("deprecation")
	public void handleOrder(CovariateData covarData) {
		String dbName = covarData.getTag();
		if (!influxDb.describeDatabases().contains(dbName)) {
			influxDb.createDatabase(dbName);
		}
		influxDb.setDatabase(dbName);
		influxDb.setRetentionPolicy("autogen");
			
		writeDataPoints(influxDb, dbName, covarData.getCovarName(), covarData.getValues());
	}
	
	/**
	 * @param influxDb
	 * @param dbName
	 * @param covarName
	 * @param values
	 */
	public void writeDataPoints(InfluxDB influxDb, String dbName, String covarName, List<CovariateValue> values) {	
		influxDb.enableBatch(BatchOptions.DEFAULTS);
		for(CovariateValue value: values) {
			Point point = null;
			if(value instanceof StringCovariateValue) {
				point = Point.measurement(covarName)
					.time(((StringCovariateValue) value).getTimestamp(), TimeUnit.MILLISECONDS)
				    .addField("value", ((StringCovariateValue) value).getValue()) 
					.build();
			} else if(value instanceof BooleanCovariateValue) {
				point = Point.measurement(covarName)
						.time(((BooleanCovariateValue) value).getTimestamp(), TimeUnit.MILLISECONDS)
					    .addField("value", ((BooleanCovariateValue) value).isValue()) 
						.build();
			} else if(value instanceof NumericalCovariateValue) {
				point = Point.measurement(covarName)
						.time(((NumericalCovariateValue) value).getTimestamp(), TimeUnit.MILLISECONDS)
					    .addField("value", ((NumericalCovariateValue) value).getValue()) 
						.build();
			}
			influxDb.write(point);
							 
		}
		influxDb.disableBatch();
	}
}
