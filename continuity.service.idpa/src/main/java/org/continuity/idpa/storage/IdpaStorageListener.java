package org.continuity.idpa.storage;

import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

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
	 * @param aid
	 *            The app-id of the changed application.
	 * @param version
	 *            The version or timestamp of the changed application.
	 */
	void onApplicationChanged(AppId aid, VersionOrTimestamp version);

	/**
	 * Will be called whenever an annotation has been changed.
	 *
	 * @param aid
	 *            The app-id of the changed annotation.
	 * @param version
	 *            The version or timestamp of the changed annotation.
	 */
	void onAnnotationChanged(AppId aid, VersionOrTimestamp version);

}
