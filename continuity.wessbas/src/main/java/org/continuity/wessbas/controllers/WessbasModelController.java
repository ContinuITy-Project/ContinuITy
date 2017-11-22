package org.continuity.wessbas.controllers;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.continuity.wessbas.entities.WorkloadModelPack;
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
@RequestMapping("wessbas")
public class WessbasModelController {

	@RequestMapping(path = "/{id}", method = RequestMethod.GET)
	public WorkloadModelPack getOverview(@PathVariable String id) {
		return new WorkloadModelPack(getModelLink(id), "/workload", "/system", "/annotation");
	}

	@RequestMapping(path = "/{id}/workload", method = RequestMethod.GET)
	public WorkloadModelStorageEntry getModel(@PathVariable String id) {
		return SimpleModelStorage.instance().get(id);
	}

	@RequestMapping(path = "/{id}", method = RequestMethod.DELETE)
	public boolean removeModel(@PathVariable String id) {
		return SimpleModelStorage.instance().remove(id);
	}

	private String getModelLink(String id) {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			hostname = "UNKNOWN";
		}

		return hostname + "/wessbas/" + id;
	}

}
