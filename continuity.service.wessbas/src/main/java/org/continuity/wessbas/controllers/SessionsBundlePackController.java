package org.continuity.wessbas.controllers;

import static org.continuity.api.rest.RestApi.Wessbas.SessionsBundles.Paths.GET;

import org.continuity.api.entities.artifact.SessionsBundlePack;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.wessbas.entities.BehaviorModelPack;
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
 * @author Alper Hidiroglu
 *
 */
@RestController()
@RequestMapping(RestApi.Wessbas.SessionsBundles.ROOT)
public class SessionsBundlePackController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionsBundlePackController.class);

	@Autowired
	private MixedStorage<BehaviorModelPack> storage;

	@RequestMapping(value = GET, method = RequestMethod.GET)
	public ResponseEntity<SessionsBundlePack> getSessionsBundlePackFromLink(@PathVariable String id) {
		BehaviorModelPack behaviorModelPack = storage.get(id);
		SessionsBundlePack sessionsBundles = behaviorModelPack.getSessionsBundlePack();

		if (sessionsBundles == null) {
			LOGGER.warn("Could not find sessions-bundle-pack for id {}!", id);
			return ResponseEntity.notFound().build();
		} else {
			LOGGER.info("Returned sessions-bundle-pack for id {}!", id);
			return ResponseEntity.ok(sessionsBundles);
		}
	}
}
