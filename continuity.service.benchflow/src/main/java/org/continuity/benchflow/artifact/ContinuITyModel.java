package org.continuity.benchflow.artifact;

import org.continuity.api.entities.artifact.BehaviorModel;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;

/**
 * Stores the required models for the transformation to the BenchFlow model.
 * 
 * @author Manuel Palenga
 *
 */
public class ContinuITyModel {

	private BehaviorModel behaviorModel;
	private Application application; 
	private ApplicationAnnotation annotation;
	
	public ContinuITyModel(BehaviorModel behaviorModel, Application application,
			ApplicationAnnotation annotation) {
		this.behaviorModel = behaviorModel;
		this.application = application;
		this.annotation = annotation;
	}
	
	public BehaviorModel getBehaviorModel() {
		return behaviorModel;
	}
	
	public Application getApplication() {
		return application;
	}
	
	public ApplicationAnnotation getAnnotation() {
		return annotation;
	}
	
}
