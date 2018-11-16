package org.continuity.forecast.controllers;

import static org.continuity.api.rest.RestApi.Forecast.ForecastResult.ROOT;
import static org.continuity.api.rest.RestApi.Forecast.ForecastResult.Paths.GET;

import org.continuity.api.entities.artifact.ForecastBundle;
import org.continuity.commons.storage.MixedStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Alper Hi
 * @author Henning Schulz
 *
 */
@RestController()
@RequestMapping(ROOT)
public class ForecastController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ForecastController.class);

	@Autowired
	private MixedStorage<ForecastBundle> storage;

	@RequestMapping(value = GET, method = RequestMethod.GET)
	public ResponseEntity<ForecastBundle> getForecastBundleFromLink(@PathVariable String id) {
		ForecastBundle bundle = storage.get(id);

		if (bundle == null) {
			LOGGER.warn("Could not find forecast for id {}!", id);
			return ResponseEntity.notFound().build();
		} else {
			LOGGER.info("Returned forecast for id {}!", id);
			return ResponseEntity.ok(bundle);
		}
	}
}
