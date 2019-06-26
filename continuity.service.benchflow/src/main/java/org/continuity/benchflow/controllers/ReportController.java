package org.continuity.benchflow.controllers;

import static org.continuity.api.rest.RestApi.BenchFlow.Report.ROOT;
import static org.continuity.api.rest.RestApi.BenchFlow.Report.Paths.DELETE;
import static org.continuity.api.rest.RestApi.BenchFlow.Report.Paths.GET;

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

/**
 * REST endpoint for test plan reports.
 *
 * @author Henning Schulz, Manuel Palenga
 *
 */
@RestController
@RequestMapping(ROOT)
public class ReportController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportController.class);

	@Autowired
	@Qualifier("reportStorage")
	private MemoryStorage<String> storage;

	/**
	 * Returns the report that is stored with the specified ID.
	 *
	 * @param id
	 *            The ID of the report.
	 * @return A report or a 404 error response if not found.
	 */
	@RequestMapping(value = GET, method = RequestMethod.GET)
	public ResponseEntity<String> getReport(@PathVariable String id) {
		String report = storage.get(id);

		if (report == null) {
			LOGGER.warn("Could not find a report with id {}!", id);
			return ResponseEntity.notFound().build();
		} else {
			LOGGER.info("Retrieved report with id {}.", id);
			return ResponseEntity.ok(report);
		}
	}

	/**
	 * Deletes the report that is stored with the specified ID.
	 *
	 * @param id
	 *            The ID of the report.
	 * @return A report or a 404 error response if not found.
	 */
	@RequestMapping(value = DELETE, method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteReport(@PathVariable String id) {
		boolean deleted = storage.remove(id);

		if (deleted) {
			LOGGER.info("Deleted report with id {}.", id);
			return ResponseEntity.ok("Deleted.");
		} else {
			LOGGER.warn("Could not delete a report with id {}!", id);
			return ResponseEntity.notFound().build();
		}
	}

}
