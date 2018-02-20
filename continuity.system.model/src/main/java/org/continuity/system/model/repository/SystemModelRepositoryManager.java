package org.continuity.system.model.repository;

import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;

import javax.ws.rs.NotSupportedException;

import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;
import org.continuity.annotation.dsl.visitor.FindById;
import org.continuity.commons.utils.DataHolder;
import org.continuity.system.model.changes.SystemChangeDetector;
import org.continuity.system.model.entities.SystemChange;
import org.continuity.system.model.entities.SystemChangeReport;
import org.continuity.system.model.entities.SystemChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the {@link SystemModelRepository}.
 *
 * @author Henning Schulz
 *
 */
public class SystemModelRepositoryManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemModelRepositoryManager.class);

	private final SystemModelRepository repository;

	public SystemModelRepositoryManager(SystemModelRepository repository) {
		this.repository = repository;
	}

	/**
	 * Saves the passed system model if something changed.
	 *
	 * @param tag
	 *            The tag of the model.
	 * @param system
	 *            The system model to be stored.
	 * @return A report containing the changes of the system model since the version before.
	 */
	public SystemChangeReport saveOrUpdate(String tag, SystemModel system) {
		return saveOrUpdate(tag, system, EnumSet.noneOf(SystemChangeType.class));
	}

	/**
	 * Saves the passed system model if something changed - ignoring a set of change types.
	 *
	 * @param tag
	 *            The tag of the model.
	 * @param system
	 *            The system model to be stored.
	 * @param ignoredChanges
	 *            Change types to be ignored.
	 * @return A report containing the changes of the system model since the latest version before.
	 *         If the model is older than the latest one, an empty report will be returned.
	 */
	public SystemChangeReport saveOrUpdate(String tag, SystemModel system, EnumSet<SystemChangeType> ignoredChanges) {
		SystemChangeDetector detector = new SystemChangeDetector(system, ignoredChanges);
		SystemChangeReport report = SystemChangeReport.empty(system.getTimestamp());

		SystemModel before = repository.readLatestBefore(tag, system.getTimestamp());

		if (before != null) {
			detector.compareTo(before);
			report = detector.getReport();
		} else {
			report = SystemChangeReport.allOf(system);
		}

		if (report.changed()) {
			SystemModel oldestAfter = repository.readOldestAfter(tag, system.getTimestamp());

			boolean changed = false;

			if (oldestAfter == null) {
				changed = true;
			} else {
				detector = new SystemChangeDetector(system, ignoredChanges);
				detector.compareTo(oldestAfter);
				changed = detector.getReport().changed();
			}

			if (changed) {
				if (!ignoredChanges.isEmpty()) {
					system = mergeIgnoredChanges(before, system, report);
				}

				try {
					repository.save(tag, system);
					LOGGER.info("Stored a new system model with tag {} and date {}.", tag, system.getTimestamp());
				} catch (IOException e) {
					LOGGER.error("Could not save the system model with tag {} and date {}!", tag, system.getTimestamp());
					LOGGER.error("Exception: ", e);
				}
			} else {
				try {
					repository.updateSystemChange(tag, oldestAfter.getTimestamp(), system.getTimestamp());
					LOGGER.info("Updated the system change of tag {} from {} to {}.", tag, oldestAfter.getTimestamp(), system.getTimestamp());
				} catch (IOException e) {
					LOGGER.error("Could not update the system change of tag {} from {} to {}", tag, oldestAfter.getTimestamp(), system.getTimestamp());
					LOGGER.error("Exception: ", e);
				}
			}
		}

		return report;
	}

	private SystemModel mergeIgnoredChanges(SystemModel oldModel, SystemModel newModel, SystemChangeReport report) {
		for (SystemChange change : report.getIgnoredSystemChanges()) {
			switch (change.getType()) {
			case INTERFACE_ADDED:
				newModel.getInterfaces().removeIf(interf -> interf.getId().equals(change.getChangedElement().getId()));
				break;
			case INTERFACE_CHANGED:
				ServiceInterface<?> oldInterf = FindById.find(change.getChangedElement().getId(), ServiceInterface.GENERIC_TYPE).in(oldModel).getFound();
				ServiceInterface<?> newInterf = FindById.find(change.getChangedElement().getId(), ServiceInterface.GENERIC_TYPE).in(newModel).getFound();

				if ((oldInterf == null) || (newInterf == null)) {
					LOGGER.error("There is a change {}, but I could not find the interface in both system versions! Ignoring the change.", change);
				} else {
					newInterf.clonePropertyFrom(change.getChangedProperty(), oldInterf);
				}
				break;
			case INTERFACE_REMOVED:
				newModel.addInterface(oldModel.getInterfaces().stream().filter(interf -> interf.getId().equals(change.getChangedElement().getId())).findFirst().get());
				break;
			case PARAMETER_CHANGED:
				throw new NotSupportedException("Ignoring PARAMETER_CHANGED is currently not supported!");
			case PARAMETER_ADDED:
				throw new NotSupportedException("Ignoring PARAMETER_ADDED is currently not supported!");
			case PARAMETER_REMOVED:
				final DataHolder<String> idHolder = new DataHolder<>();
				final DataHolder<Parameter> paramHolder = new DataHolder<>();

				new ContinuityByClassSearcher<>(ServiceInterface.GENERIC_TYPE, interf -> {
					final Parameter param = FindById.find(change.getChangedElement().getId(), Parameter.class).in(interf).getFound();

					if (param != null) {
						idHolder.set(interf.getId());
						paramHolder.set(param);
					}
				}).visit(oldModel);

				ServiceInterface<?> newInterface = FindById.find(idHolder.get(), ServiceInterface.GENERIC_TYPE).in(newModel).getFound();

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
	private <P extends Parameter> void addParameter(Parameter param, ServiceInterface<P> interf) {
		interf.addParameter((P) param);
	}

	/**
	 * Reads the current system model.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @return The current system model with the tag.
	 * @throws IOException
	 *             If an error occurs during reading.
	 */
	public SystemModel read(String tag) throws IOException {
		return repository.readLatest(tag);
	}

	/**
	 * Retrieves the delta (that is, the list of changes) since the specified date.
	 *
	 * @param tag
	 *            The tag of the system models.
	 * @param date
	 *            The date since when the changes are to be determined.
	 * @return A report holding the changes.
	 */
	public SystemChangeReport getDeltaSince(String tag, Date date) {
		SystemModel latest = repository.readLatest(tag);

		if (latest == null) {
			return SystemChangeReport.empty(date);
		}

		SystemModel latestBefore = repository.readLatestBefore(tag, date);

		if (latestBefore == null) {
			latestBefore = new SystemModel();
		}

		SystemChangeDetector checker = new SystemChangeDetector(latest);
		checker.compareTo(latestBefore);

		return checker.getReport();
	}

}
