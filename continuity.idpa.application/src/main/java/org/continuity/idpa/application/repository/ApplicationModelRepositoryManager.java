package org.continuity.idpa.application.repository;

import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;

import javax.ws.rs.NotSupportedException;

import org.continuity.api.entities.report.ApplicationChange;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.commons.utils.DataHolder;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.application.changes.ApplicationChangeDetector;
import org.continuity.idpa.legacy.IdpaFromOldAnnotationConverter;
import org.continuity.idpa.visitor.FindBy;
import org.continuity.idpa.visitor.IdpaByClassSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the {@link ApplicationModelRepository}.
 *
 * @author Henning Schulz
 *
 */
public class ApplicationModelRepositoryManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationModelRepositoryManager.class);

	private final ApplicationModelRepository repository;

	public ApplicationModelRepositoryManager(ApplicationModelRepository repository) {
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

		Application before = repository.readLatestBefore(tag, application.getTimestamp());

		if (before != null) {
			detector.compareTo(before);
			report = detector.getReport();
		} else {
			report = ApplicationChangeReport.allOf(application);
		}

		if (report.changed()) {
			Application oldestAfter = repository.readOldestAfter(tag, application.getTimestamp());

			boolean changed = false;

			if (oldestAfter == null) {
				changed = true;
			} else {
				detector = new ApplicationChangeDetector(application, ignoredChanges);
				detector.compareTo(oldestAfter);
				changed = detector.getReport().changed();
			}

			if (changed) {
				if (!ignoredChanges.isEmpty()) {
					application = mergeIgnoredChanges(before, application, report);
				}

				try {
					repository.save(tag, application);
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
		}

		return report;
	}

	private Application mergeIgnoredChanges(Application oldModel, Application newModel, ApplicationChangeReport report) {
		for (ApplicationChange change : report.getIgnoredApplicationChanges()) {
			switch (change.getType()) {
			case ENDPOINT_ADDED:
				newModel.getEndpoints().removeIf(interf -> interf.getId().equals(change.getChangedElement().getId()));
				break;
			case ENDPOINT_CHANGED:
				Endpoint<?> oldInterf = FindBy.findById(change.getChangedElement().getId(), Endpoint.GENERIC_TYPE).in(oldModel).getFound();
				Endpoint<?> newInterf = FindBy.findById(change.getChangedElement().getId(), Endpoint.GENERIC_TYPE).in(newModel).getFound();

				if ((oldInterf == null) || (newInterf == null)) {
					LOGGER.error("There is a change {}, but I could not find the interface in both application versions! Ignoring the change.", change);
				} else {
					newInterf.clonePropertyFrom(change.getChangedProperty(), oldInterf);
				}
				break;
			case ENDPOINT_REMOVED:
				newModel.addEndpoint(oldModel.getEndpoints().stream().filter(interf -> interf.getId().equals(change.getChangedElement().getId())).findFirst().get());
				break;
			case PARAMETER_CHANGED:
				throw new NotSupportedException("Ignoring PARAMETER_CHANGED is currently not supported!");
			case PARAMETER_ADDED:
				throw new NotSupportedException("Ignoring PARAMETER_ADDED is currently not supported!");
			case PARAMETER_REMOVED:
				final DataHolder<String> idHolder = new DataHolder<>();
				final DataHolder<Parameter> paramHolder = new DataHolder<>();

				new IdpaByClassSearcher<>(Endpoint.GENERIC_TYPE, interf -> {
					final Parameter param = FindBy.findById(change.getChangedElement().getId(), Parameter.class).in(interf).getFound();

					if (param != null) {
						idHolder.set(interf.getId());
						paramHolder.set(param);
					}
				}).visit(oldModel);

				Endpoint<?> newInterface = FindBy.findById(idHolder.get(), Endpoint.GENERIC_TYPE).in(newModel).getFound();

				if (newInterface != null) {
					addParameter(paramHolder.get(), newInterface);
				}

				break;
			default:
				break;
			}
		}

		return newModel;
	}

	@SuppressWarnings("unchecked")
	private <P extends Parameter> void addParameter(Parameter param, Endpoint<P> interf) {
		interf.addParameter((P) param);
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
		return repository.readLatest(tag);
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
		Application latest = repository.readLatest(tag);

		if (latest == null) {
			return ApplicationChangeReport.empty(date);
		}

		Application latestBefore = repository.readLatestBefore(tag, date);

		if (latestBefore == null) {
			latestBefore = new Application();
		}

		ApplicationChangeDetector checker = new ApplicationChangeDetector(latest);
		checker.compareTo(latestBefore);

		return checker.getReport();
	}

	/**
	 * Updates all legacy applications of version lower than 1.0.
	 *
	 * @param tag
	 *            The tag of the applications.
	 * @return The number of updated annotations.
	 * @throws IOException
	 *             If an error during reading occurs.
	 */
	public int updateAllLegacyApplications(String tag) throws IOException {
		IdpaFromOldAnnotationConverter converter = new IdpaFromOldAnnotationConverter();

		int i = 0;

		for (String legacyApplication : repository.readLegacyApplications(tag)) {
			Application application = converter.convertFromSystemModel(legacyApplication);
			repository.save(tag, application);
			i++;
		}

		return i;
	}

}
