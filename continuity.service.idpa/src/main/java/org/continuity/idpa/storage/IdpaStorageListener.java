package org.continuity.idpa.storage;

import java.util.Date;

/**
 * Common interface for listeners on changes in an {@link IdpaStorage}.
 *
 * @author Henning Schulz
 *
 */
public interface IdpaStorageListener {

	/**
	 * Will be called whenever an application has been changed.
	 *
	 * @param tag
	 *            The tag of the changed application.
	 * @param timestamp
	 *            The timestamp of the changed application.
	 */
	void onApplicationChanged(String tag, Date timestamp);

	/**
	 * Will be called whenever an annotation has been changed.
	 *
	 * @param tag
	 *            The tag of the changed annotation.
	 * @param timestamp
	 *            The timestamp of the changed annotation.
	 */
	void onAnnotationChanged(String tag, Date timestamp);

}
