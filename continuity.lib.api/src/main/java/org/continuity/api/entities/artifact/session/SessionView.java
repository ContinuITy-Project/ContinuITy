package org.continuity.api.entities.artifact.session;

public class SessionView {

	/**
	 * Only shows the core properties of a session.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Simple {
	}

	/**
	 * Shows everything of {@link Simple} and extended information per request in addition.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Extended extends Simple {
	}

	/**
	 * Shows everything of {@link Extended} and internal information in addition.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Internal extends Extended {
	}

}
