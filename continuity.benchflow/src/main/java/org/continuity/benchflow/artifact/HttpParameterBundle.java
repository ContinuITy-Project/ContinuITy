package org.continuity.benchflow.artifact;

import java.util.HashMap;
import java.util.Map;

import cloud.benchflow.dsl.definition.workload.operation.body.BodyType;
import cloud.benchflow.dsl.definition.workload.operation.parameter.Parameter;

/**
 * 
 * @author Manuel Palenga
 *
 */
public class HttpParameterBundle {
	
	private Map<String, Parameter> queryParameter = null;
	private Map<String, Parameter> urlParameter = null;
	private BodyType bodyInput = null;
	
	public HttpParameterBundle() {
		queryParameter = new HashMap<String, Parameter>();
		urlParameter = new HashMap<String, Parameter>();
		bodyInput = null;
	}
	
	public Map<String, Parameter> getQueryParameter() {
		return queryParameter;
	}
	
	public Map<String, Parameter> getUrlParameter() {
		return urlParameter;
	}

	public void setBodyInput(BodyType bodyInput) {
		this.bodyInput = bodyInput;
	}
	
	public BodyType getBodyInput() {
		return bodyInput;
	}
	
}
