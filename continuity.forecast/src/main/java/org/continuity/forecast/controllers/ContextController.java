package org.continuity.forecast.controllers;

import static org.continuity.api.rest.RestApi.Forecast.Context.ROOT;
import static org.continuity.api.rest.RestApi.Forecast.Context.Paths.SUBMIT;

import org.continuity.forecast.context.CovariateData;
import org.continuity.forecast.managers.CovariateDataManager;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example json input: {"tag":"appl-name","covar-name":"covar-name", "values":[{"timestamp": 123456789, "value": "some value"}]}
 * @author Alper Hidiroglu
 *
 */
@RestController()
@RequestMapping(ROOT)
public class ContextController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContextController.class);

	@RequestMapping(value = SUBMIT, method = RequestMethod.POST)
	public ResponseEntity<String> getData(@RequestBody CovariateData covarData) {
		LOGGER.info("Received new order to process");
		InfluxDB influxDb = InfluxDBFactory.connect("http://127.0.0.1:8086", "admin", "admin");
		CovariateDataManager manager = new CovariateDataManager(influxDb);
		manager.handleOrder(covarData);
		influxDb.close();
		return new ResponseEntity<String>(covarData.toString(), HttpStatus.OK);
	}
}
