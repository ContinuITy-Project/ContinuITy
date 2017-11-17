package org.continuity.wessbas.controllers;

import org.continuity.wessbas.entities.WorkloadModelStorageEntry;
import org.continuity.wessbas.storage.SimpleModelStorage;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(WessbasModelController.BASE_PATH)
public class WessbasModelController {

	public static final String BASE_PATH = "/wessbas/model";

	@RequestMapping(path = "/{id}", method = RequestMethod.GET)
	public WorkloadModelStorageEntry getModel(@PathVariable String id) {
		return SimpleModelStorage.instance().get(id);
	}

	@RequestMapping(path = "/{id}", method = RequestMethod.DELETE)
	public boolean removeModel(@PathVariable String id) {
		return SimpleModelStorage.instance().remove(id);
	}

}
