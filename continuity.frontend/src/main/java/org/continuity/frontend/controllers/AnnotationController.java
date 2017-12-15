package org.continuity.frontend.controllers;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping("annotation")
public class AnnotationController {

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Gets the system model for the specified tag.
	 *
	 * @param tag
	 *            The tag of the model.
	 * @return The system model.
	 */
	@RequestMapping(path = "{tag}/system", method = RequestMethod.GET)
	public ResponseEntity<SystemModel> getSystemModel(@PathVariable("tag") String tag) {
		return restTemplate.getForEntity("http://workload-annotation/ann/" + tag + "/system", SystemModel.class);
	}

	/**
	 * Gets the annotation for the specified tag.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return The annotation.
	 */
	@RequestMapping(path = "{tag}/annotation", method = RequestMethod.GET)
	public ResponseEntity<SystemAnnotation> getAnnotation(@PathVariable("tag") String tag) {
		return restTemplate.getForEntity("http://workload-annotation/ann/" + tag + "/annotation", SystemAnnotation.class);
	}

	/**
	 * Updates the annotation for the specified tag.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param annotation
	 *            The annotation.
	 */
	@RequestMapping(path = "{tag}/annotation", method = RequestMethod.POST)
	public ResponseEntity<String> updateAnnotation(@PathVariable("tag") String tag, @RequestBody SystemAnnotation annotation) {
		return restTemplate.postForEntity("http://workload-annotation/ann/" + tag + "/annotation", annotation, String.class);
	}


}
