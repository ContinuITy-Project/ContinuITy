package org.continuity.idpa.storage;

import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;

import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.commons.idpa.ApplicationChangeDetector;
import org.continuity.commons.idpa.ApplicationUpdater;
import org.continuity.idpa.Idpa;
import org.continuity.idpa.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the applications stored in an {@link IdpaStorage}.
 *
 * @author Henning Schulz
 *
 */
public class ApplicationStorageManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationStorageManager.class);

	private final IdpaStorage repository;

	public ApplicationStorageManager(IdpaStorage repository) {
		this.repository = repository;
	}

	/**
	 * Saves the passed application model if something changed.
	 *
	 * @param tag
	 *            The tag of the model.
	 * @param application
	 *            The application model to be stored.
	 * @return A report containing the changes of the application model since the version before.
	 */
	public ApplicationChangeReport saveOrUpdate(String tag, Application application) {
		return saveOrUpdate(tag, application, EnumSet.noneOf(ApplicationChangeType.class));
	}

	/**
	 * Saves the passed application model if something changed - ignoring a set of change types.
	 *
	 * @param tag
	 *            The tag of the model.
	 * @param application
	 *            The application model to be stored.
	 * @param ignoredChanges
	 *            Change types to be ignored.
	 * @return A report containing the changes of the application model since the latest version
	 *         before. If the model is older than the latest one, an empty report will be returned.
	 */
	public ApplicationChangeReport saveOrUpdate(String tag, Application application, EnumSet<ApplicationChangeType> ignoredChanges) {
		ApplicationChangeDetector detector = new ApplicationChangeDetector(application, ignoredChanges);
		ApplicationChangeReport report = ApplicationChangeReport.empty(application.getTimestamp());

		Idpa before = repository.readLatestBefore(tag, application.getTimestamp());

		if (before != null) {
			detector.compareTo(before.getApplication());
			report = detector.getReport();
		} else {
			report = ApplicationChangeReport.allOf(application);
		}

		if (report.changed()) {
			Idpa oldestAfter = repository.readOldestAfter(tag, application.getTimestamp());

			boolean changed = false;

			if (oldestAfter == null) {
				changed = true;
			} else {
				detector = new ApplicationChangeDetector(application, ignoredChanges);
				detector.compareTo(oldestAfter.getApplication());
				changed = detector.getReport().changed();
			}

			if (changed) {
				Application updatedApplication;

				if (before == null) {
					updatedApplication = application;
				} else {
					EnumSet<ApplicationChangeType> consideredChangeTypes = EnumSet.allOf(ApplicationChangeType.class);
					consideredChangeTypes.removeAll(ignoredChanges);

					ApplicationUpdater updater = new ApplicationUpdater();
					report = updater.updateApplication(before.getApplication(), application, consideredChangeTypes);
					updatedApplication = report.getUpdatedApplication();
				}

				try {
					repository.save(tag, updatedApplication);
					LOGGER.info("Stored a new application model with tag {} and date {}.", tag, application.getTimestamp());
				} catch (IOException e) {
					LOGGER.error("Could not save the application model with tag {} and date {}!", tag, application.getTimestamp());
					LOGGER.error("Exception: ", e);
				}
			} else {
				try {
					repository.updateApplicationChange(tag, oldestAfter.getTimestamp(), application.getTimestamp());
					LOGGER.info("Updated the application change of tag {} from {} to {}.", tag, oldestAfter.getTimestamp(), application.getTimestamp());
				} catch (IOException e) {
					LOGGER.error("Could not update the application change of tag {} from {} to {}", tag, oldestAfter.getTimestamp(), application.getTimestamp());
					LOGGER.error("Exception: ", e);
				}
			}
		} else {
			LOGGER.info("Nothing changed for tag {} and timestamp {} compared to {}.", tag, application.getTimestamp(), (before == null ? "(none)" : before.getApplication().getTimestamp()));
		}

		return report;
	}

	/**
	 * Reads the current application model.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @return The current application model with the tag.
	 * @throws IOException
	 *             If an error occurs during reading.
	 */
	public Application read(String tag) throws IOException {
		return repository.readLatest(tag).getApplication();
	}

	/**
	 * Reads the application model that is stored for the given timestamp.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param timestamp
	 *            The timestamp for which an application model is searched.
	 * @return The current application model with the tag.
	 * @throws IOException
	 *             If an error occurs during reading.
	 */
	public Application read(String tag, Date timestamp) throws IOException {
		return repository.readLatestBefore(tag, timestamp).getApplication();
	}

	/**
	 * Retrieves the delta (that is, the list of changes) since the specified date.
	 *
	 * @param tag
	 *            The tag of the application models.
	 * @param date
	 *            The date since when the changes are to be determined.
	 * @return A report holding the changes.
	 */
	public ApplicationChangeReport getDeltaSince(String tag, Date date) {
		Application latest = repository.readLatest(tag).getApplication();

		if (latest == null) {
			return ApplicationChangeReport.empty(date);
		}

		Application latestBefore = repository.readLatestBefore(tag, date).getApplication();

		if (latestBefore == null) {
			latestBefore = new Application();
		}

		ApplicationChangeDetector checker = new ApplicationChangeDetector(latest);
		checker.compareTo(latestBefore);

		return checker.getReport();
	}

}
