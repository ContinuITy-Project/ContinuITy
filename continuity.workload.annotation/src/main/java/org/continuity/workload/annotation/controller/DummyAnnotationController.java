package org.continuity.workload.annotation.controller;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("annotation/dummy")
public class DummyAnnotationController {

	@RequestMapping("dvdstore/annotation")
	public SystemAnnotation getDvdStoreAnnotation() {
		return ContinuityModelTestInstance.DVDSTORE_PARSED.getAnnotation();
	}

	@RequestMapping("dvdstore/system")
	public SystemModel getDvdStoreSystem() {
		return ContinuityModelTestInstance.DVDSTORE_PARSED.getSystemModel();
	}

}
