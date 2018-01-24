package org.continuity.system.model.changes;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;
import org.continuity.system.model.entities.ModelElementReference;
import org.continuity.system.model.entities.SystemChange;
import org.continuity.system.model.entities.SystemChangeReport;
import org.continuity.system.model.entities.SystemChangeType;

/**
 * Compares system models against a base system model. E.g., can be used to determine the
 * differences of an old system model and the new one.
 *
 * @author Henning Schulz
 *
 */
public class SystemChangeDetector {

	private final SystemModel newSystemModel;

	private final SystemChangeReportBuilder reportBuilder;

	/**
	 * Creates an instance with the current system model as base.
	 *
	 * @param newSystemModel
	 *            The current system model.
	 */
	public SystemChangeDetector(SystemModel newSystemModel) {
		this(newSystemModel, EnumSet.noneOf(SystemChangeType.class));
	}

	public SystemChangeDetector(SystemModel newSystemModel, EnumSet<SystemChangeType> ignoredChangeTypes) {
		this.newSystemModel = newSystemModel;
		this.reportBuilder = new SystemChangeReportBuilder(ignoredChangeTypes);
	}

	/**
	 * Compares an old system model to the stored one and reports differences.
	 *
	 * @param oldSystemModel
	 *            An old system model.
	 */
	public void compareTo(SystemModel oldSystemModel) {
		final Set<ModelElementReference> visited = new HashSet<>();
		ContinuityByClassSearcher<ServiceInterface<?>> searcher = new ContinuityByClassSearcher<>(ServiceInterface.GENERIC_TYPE, inter -> checkInterface(inter, oldSystemModel, visited));
		searcher.visit(newSystemModel);

		searcher = new ContinuityByClassSearcher<>(ServiceInterface.GENERIC_TYPE, inter -> reportRemovedInterface(inter, visited));
		searcher.visit(oldSystemModel);
	}

	private boolean checkInterface(ServiceInterface<?> newInterf, SystemModel oldSystemModel, Set<ModelElementReference> visited) {
		final Holder<ServiceInterface<?>> interfHolder = new Holder<>();
		ContinuityByClassSearcher<ServiceInterface<?>> searcher = new ContinuityByClassSearcher<>(ServiceInterface.GENERIC_TYPE, oldInterf -> {
			if (oldInterf.getId().equals(newInterf.getId())) {
				interfHolder.element = oldInterf;
			}
		});
		searcher.visit(oldSystemModel);

		ModelElementReference ref = new ModelElementReference(newInterf);

		if (interfHolder.element == null) {
			reportBuilder.addChange(new SystemChange(SystemChangeType.INTERFACE_ADDED, ref));
		} else {
			ServiceInterface<?> oldInterf = interfHolder.element;

			if (!oldInterf.equals(newInterf)) {
				reportBuilder.addChange(new SystemChange(SystemChangeType.INTERFACE_CHANGED, ref));
			}

			checkParameters(oldInterf, newInterf);

			visited.add(ref);
		}

		return true;
	}

	private boolean reportRemovedInterface(ServiceInterface<?> oldInterf, Set<ModelElementReference> visited) {
		ModelElementReference ref = new ModelElementReference(oldInterf);
		if (!visited.contains(ref)) {
			reportBuilder.addChange(new SystemChange(SystemChangeType.INTERFACE_REMOVED, ref));
		}

		return true;
	}

	private void checkParameters(ServiceInterface<?> oldInterf, ServiceInterface<?> newInterf) {
		if (CollectionUtils.isEqualCollection(oldInterf.getParameters(), newInterf.getParameters())) {
			return;
		}

		for (Parameter param : newInterf.getParameters()) {
			if (!oldInterf.getParameters().contains(param)) {
				reportBuilder.addChange(new SystemChange(SystemChangeType.PARAMETER_ADDED, new ModelElementReference(param)));
			}
		}

		for (Parameter param : oldInterf.getParameters()) {
			if (!newInterf.getParameters().contains(param)) {
				reportBuilder.addChange(new SystemChange(SystemChangeType.PARAMETER_REMOVED, new ModelElementReference(param)));
			}
		}
	}

	/**
	 * Gets a report based on the evaluations done before.
	 *
	 * @return The report.
	 */
	public SystemChangeReport getReport() {
		return reportBuilder.buildReport();
	}

	private static class Holder<T> {
		T element;
	}
}
