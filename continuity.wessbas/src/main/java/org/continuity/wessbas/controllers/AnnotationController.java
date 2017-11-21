package org.continuity.wessbas.controllers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.wessbas.storage.SimpleModelStorage;
import org.continuity.wessbas.transform.annotation.AnnotationFromWessbasExtractor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("wessbas/annotation")
public class AnnotationController {

	private final ConcurrentMap<String, SystemModel> systemModelBuffer = new ConcurrentHashMap<>();

	private final ConcurrentMap<String, SystemAnnotation> annotationBuffer = new ConcurrentHashMap<>();

	@RequestMapping(path = "{id}/system", method = RequestMethod.GET)
	public SystemModel getSystemModel(@PathVariable String id) {
		SystemModel systemModel = systemModelBuffer.get(id);

		if (systemModel == null) {
			AnnotationFromWessbasExtractor annotationExtractor = new AnnotationFromWessbasExtractor();
			annotationExtractor.init(SimpleModelStorage.instance().get(id).getWorkloadModel(), id);

			systemModel = annotationExtractor.extractSystemModel();
			SystemAnnotation annotation = annotationExtractor.extractInitialAnnotation();

			annotationBuffer.put(id, annotation);
		}

		return systemModel;
	}

	@RequestMapping(path = "{id}/annotation", method = RequestMethod.GET)
	public SystemAnnotation getSystemAnnotation(@PathVariable String id) {
		SystemAnnotation annotation = annotationBuffer.get(id);

		if (annotation == null) {
			AnnotationFromWessbasExtractor annotationExtractor = new AnnotationFromWessbasExtractor();
			annotationExtractor.init(SimpleModelStorage.instance().get(id).getWorkloadModel(), id);

			annotation = annotationExtractor.extractInitialAnnotation();
			SystemModel systemModel = annotationExtractor.extractSystemModel();

			systemModelBuffer.put(id, systemModel);
		}

		return annotation;
	}

}
