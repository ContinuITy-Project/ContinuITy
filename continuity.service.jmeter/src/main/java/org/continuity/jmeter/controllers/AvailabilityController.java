package org.continuity.jmeter.controllers;

import static org.continuity.api.rest.RestApi.JMeter.Availability.ROOT;
import static org.continuity.api.rest.RestApi.JMeter.Availability.Paths.CHECK;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for checking the availability of this service.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class AvailabilityController {

	@RequestMapping(path = CHECK, method = RequestMethod.GET)
	public String checkAvailability() {
		return "available";
	}

}
