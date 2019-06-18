package org.continuity.idpa;

import java.util.Date;
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

	private Date timestamp;

	private Application application;

	private ApplicationAnnotation annotation;

	private final Map<String, Boolean> additionalFlags = new HashMap<>();

	public Idpa() {
	}

	public Idpa(Date timestamp, Application application, ApplicationAnnotation annotation) {
		this.timestamp = timestamp;
		this.application = application;
		this.annotation = annotation;

		if ((this.timestamp == null) && (this.application != null)) {
			this.timestamp = this.application.getTimestamp();
		}
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
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
