package org.continuity.workload.annotation.controllers;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("dummy/dvdstore")
public class DummyAnnotationController {

	@RequestMapping(path = "annotation", method = RequestMethod.GET)
	public SystemAnnotation getDvdStoreAnnotation() {
		return ContinuityModelTestInstance.DVDSTORE_PARSED.getAnnotation();
	}

	@RequestMapping(path = "system", method = RequestMethod.GET)
	public SystemModel getDvdStoreSystem() {
		return ContinuityModelTestInstance.DVDSTORE_PARSED.getSystemModel();
	}

}
