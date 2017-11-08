package org.continuity.wessbas.controllers;

import org.continuity.wessbas.storage.SimpleModelStorage;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
@RestController
public class WessbasModelController {

	@RequestMapping(path = "/{id}", method = RequestMethod.GET)
	public WorkloadModel getModel(@PathVariable String id) {
		return SimpleModelStorage.instance().get(id);
	}

	@RequestMapping(path = "/{id}", method = RequestMethod.DELETE)
	public boolean removeModel(@PathVariable String id) {
		return SimpleModelStorage.instance().remove(id);
	}

}
