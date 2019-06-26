package org.continuity.idpa;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;

public class DynamicIdpaTestInstance implements IdpaTestInstance {

	private final Application application;

	private final ApplicationAnnotation annotation;

	public DynamicIdpaTestInstance(Application application, ApplicationAnnotation annotation) {
		this.application = application;
		this.annotation = annotation;
	}

	public DynamicIdpaTestInstance(IdpaTestInstance other) {
		this(other.getApplication(), other.getAnnotation());
	}

	@Override
	public Application getApplication() {
		return application;
	}

	@Override
	public ApplicationAnnotation getAnnotation() {
		return annotation;
	}

}
