package org.continuity.idpa;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;

public interface IdpaTestInstance {

	Application getApplication();

	ApplicationAnnotation getAnnotation();

}
