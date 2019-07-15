package org.continuity.idpa.controllers;

import static org.continuity.api.rest.RestApi.Idpa.Version.ROOT;
import static org.continuity.api.rest.RestApi.Idpa.Version.Paths.GET_LATEST;

import org.continuity.idpa.AppId;
import org.continuity.idpa.Idpa;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.storage.IdpaStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Provides information about the stored IDPA versions.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class VersionController {

	@Autowired
	private IdpaStorage storage;

	/**
	 * Returns the latest version (or timestamp) stored for a certain app-id.
	 *
	 * @param aid
	 *            The app-id.
	 * @return The version or timestamp.
	 */
	@RequestMapping(value = GET_LATEST, method = RequestMethod.GET)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<VersionOrTimestamp> getLatest(@ApiIgnore @PathVariable("app-id") AppId aid) {
		Idpa idpa = storage.readLatest(aid);

		if (idpa == null) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(idpa.getVersionOrTimestamp());
		}
	}

}
