package org.continuity.benchflow.controllers;

import static org.continuity.api.rest.RestApi.BenchFlow.DSL.ROOT;
import static org.continuity.api.rest.RestApi.BenchFlow.DSL.Paths.GET;

import org.continuity.commons.storage.MemoryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import cloud.benchflow.dsl.definition.workload.HttpWorkload;
import cloud.benchflow.dsl.definition.workload.WorkloadYamlProtocol;
import net.jcazevedo.moultingyaml.YamlValue;

/**
 * REST endpoint for BenchFlow DSLs.
 *
 * @author Henning Schulz, Manuel Palenga
 *
 */
@RestController
@RequestMapping(ROOT)
public class BenchFlowController {

	private static final Logger LOGGER = LoggerFactory.getLogger(BenchFlowController.class);

	@Autowired
	@Qualifier("benchflowDSLStorage")
	private MemoryStorage<HttpWorkload> storage;

	/**
	 * Returns the BenchFlow DSL that is stored with the specified ID.
	 *
	 * @param id
	 *            The ID of the BenchFlow DSL.
	 * @return A bundle holding the BenchFlow DSL or a 404 error response if not found.
	 */
	@RequestMapping(value = GET, method = RequestMethod.GET)
	public ResponseEntity<String> getBenchFlowDSL(@PathVariable String id) {
		HttpWorkload bundle = storage.get(id);

		if (bundle == null) {
			LOGGER.warn("Could not find a BenchFlow DSL with id {}!", id);
			return ResponseEntity.notFound().build();
		} else {
			LOGGER.info("Retrieved BenchFlow DSL with id {}.", id);
			WorkloadYamlProtocol.WorkloadWriteFormat$ writer = new WorkloadYamlProtocol.WorkloadWriteFormat$();
			YamlValue resultConfiguration = writer.write(bundle);
			return ResponseEntity.ok(resultConfiguration.prettyPrint());
		}
	}

}
