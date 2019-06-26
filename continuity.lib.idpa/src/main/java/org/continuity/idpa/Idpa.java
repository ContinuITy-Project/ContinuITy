package org.continuity.idpa;

import java.util.HashMap;
import java.util.Map;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;

/**
 * Represents an IDPA consisting of an application and an annotation.
 *
 * @author Henning Schulz
 *
 */
public class Idpa {

	private VersionOrTimestamp version;

	private Application application;

	private ApplicationAnnotation annotation;

	private final Map<String, Boolean> additionalFlags = new HashMap<>();

	public Idpa() {
	}

	public Idpa(VersionOrTimestamp version, Application application, ApplicationAnnotation annotation) {
		this.version = version;
		this.application = application;
		this.annotation = annotation;

		if ((this.version == null) && (this.application != null)) {
			this.version = this.application.getVersionOrTimestamp();
		}
	}

	public VersionOrTimestamp getVersionOrTimestamp() {
		return version;
	}

	public void setVersionOrTimestamp(VersionOrTimestamp version) {
		this.version = version;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public ApplicationAnnotation getAnnotation() {
		return annotation;
	}

	public void setAnnotation(ApplicationAnnotation annotation) {
		this.annotation = annotation;
	}

	public void addAdditionalFlag(String key, boolean value) {
		additionalFlags.put(key, value);
	}

	public boolean checkAdditionalFlag(String key) {
		Boolean value = additionalFlags.get(key);
		return value == null ? false : value;
	}

}
