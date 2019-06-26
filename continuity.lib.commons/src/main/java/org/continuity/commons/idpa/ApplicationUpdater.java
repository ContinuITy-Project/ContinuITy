package org.continuity.commons.idpa;

import java.util.EnumSet;

import javax.ws.rs.NotSupportedException;

import org.continuity.api.entities.report.ApplicationChange;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.commons.utils.DataHolder;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.visitor.FindBy;
import org.continuity.idpa.visitor.IdpaByClassSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates application models.
 *
 * @author Henning Schulz
 *
 */
public class ApplicationUpdater {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationUpdater.class);

	/**
	 * Merges an original and a new version of an application model and reports about the changes by
	 * considering a specified set of change types.
	 *
	 * @param origApp
	 *            The original version of the application model.
	 * @param newApp
	 *            The new version of the application model.
	 * @param consideredChangeTypes
	 *            The set of change types to be considered. All changes of types that are not
	 *            considered won't be contained in the updated model.
	 * @return A report about the applied changes. The updated application model will be contained
	 *         in {@link ApplicationChangeReport#getUpdatedApplication()}.
	 */
	public ApplicationChangeReport updateApplication(Application origApp, Application newApp, EnumSet<ApplicationChangeType> consideredChangeTypes) {
		Application merged = new Application();
		merged.setId(origApp.getId()); // Using oldApp is by intention
		merged.setVersionOrTimestamp(newApp.getVersionOrTimestamp());
		merged.getEndpoints().addAll(newApp.getEndpoints());

		EnumSet<ApplicationChangeType> ignoredChanges = EnumSet.allOf(ApplicationChangeType.class);
		ignoredChanges.removeAll(consideredChangeTypes);

		ApplicationChangeDetector detector = new ApplicationChangeDetector(merged, ignoredChanges);
		detector.compareTo(origApp);
		ApplicationChangeReport report = detector.getReport();

		if ((ignoredChanges != null) && !ignoredChanges.isEmpty()) {
			undoIgnoredChanges(origApp, merged, report);
		}

		report.setUpdatedApplication(merged);

		return report;
	}

	private Application undoIgnoredChanges(Application oldModel, Application newModel, ApplicationChangeReport report) {
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

}
