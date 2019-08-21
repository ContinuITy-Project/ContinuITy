package org.continuity.api.exception;

/**
 * Represents an error related to service configurations.
 * @author Henning Schulz
 *
 */
public class ServiceConfigurationException extends Exception {

	private static final long serialVersionUID = 2350734360042584798L;

	public ServiceConfigurationException() {
		super();
	}

	public ServiceConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceConfigurationException(String message) {
		super(message);
	}

	public ServiceConfigurationException(Throwable cause) {
		super(cause);
	}

}
