package org.continuity.system.model.changes;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;
import org.continuity.system.model.entities.ModelElementReference;
import org.continuity.system.model.entities.SystemChange;
import org.continuity.system.model.entities.SystemChangeReport;
import org.continuity.system.model.entities.SystemChangeType;

import com.google.common.base.Objects;

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
		this.reportBuilder = new SystemChangeReportBuilder(ignoredChangeTypes, newSystemModel.getTimestamp());
	}

	/**
	 * Compares an old system model to the stored one and reports differences.
	 *
	 * @param oldSystemModel
	 *            An old system model.
	 */
	public void compareTo(SystemModel oldSystemModel) {
		reportBuilder.setBeforeChange(oldSystemModel.getTimestamp());

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

			for (String changedProperty : oldInterf.getDifferingProperties(newInterf)) {
				if (!"parameters".equals(changedProperty)) {
					reportBuilder.addChange(new SystemChange(SystemChangeType.INTERFACE_CHANGED, ref, changedProperty));
				}
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
			ModelElementReference ref = new ModelElementReference(param);
			List<Parameter> oldParams = oldInterf.getParameters().stream().filter(p -> Objects.equal(param.getId(), p.getId())).collect(Collectors.toList());

			if (oldParams.isEmpty()) {
				reportBuilder.addChange(new SystemChange(SystemChangeType.PARAMETER_ADDED, ref));
			} else {
				for (String changedProperty : param.getDifferingProperties(oldParams.get(0))) {
					reportBuilder.addChange(new SystemChange(SystemChangeType.PARAMETER_CHANGED, ref, changedProperty));
				}
			}
		}

		for (Parameter param : oldInterf.getParameters()) {
			List<Parameter> newParams = newInterf.getParameters().stream().filter(p -> Objects.equal(param.getId(), p.getId())).collect(Collectors.toList());

			if (newParams.isEmpty()) {
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
