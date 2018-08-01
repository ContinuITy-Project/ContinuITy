package org.continuity.orchestrator.controllers;

import static org.continuity.api.rest.RestApi.Orchestrator.WorkloadModel.ROOT;
import static org.continuity.api.rest.RestApi.Orchestrator.WorkloadModel.Paths.GET;
import static org.continuity.api.rest.RestApi.Orchestrator.WorkloadModel.Paths.PERSIST;

import org.continuity.api.rest.RestApi.Generic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Controls workload models.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class WorkloadModelController {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadModelController.class);

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Retrieves the created workload model of the specified type and id. It does not wait if the
	 * model is not yet created.
	 *
	 * @param request
	 *            The request.
	 * @return The workload model.
	 */
	@RequestMapping(path = GET, method = RequestMethod.GET)
	public ResponseEntity<JsonNode> getWorkloadModel(@PathVariable String type, @PathVariable String id) {
		LOGGER.info("Trying to get the workload model from {}", Generic.WORKLOAD_MODEL_LINK.get(type).path(id));
		return restTemplate.getForEntity(Generic.WORKLOAD_MODEL_LINK.get(type).requestUrl(id).get(), JsonNode.class);
	}

	/**
	 * Persists the workload model.
	 *
	 * @param type
	 *            The workload model type (e.g., wessbas).
	 * @param id
	 *            The ID of the workload model.
	 * @return The created file's name.
	 */
	@RequestMapping(path = PERSIST, method = RequestMethod.POST)
	public ResponseEntity<String> persistWorkloadModel(@PathVariable String type, @PathVariable String id) {
		LOGGER.info("Trying to persist the workload model from {}", Generic.WORKLOAD_MODEL_LINK.get(type).path(id));
		return restTemplate.postForEntity(Generic.PERSIST_WORKLOAD_MODEL.get(type).requestUrl(id).get(), null, String.class);
	}

}
