package org.continuity.commons.idpa;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.continuity.api.entities.report.ApplicationChange;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.api.entities.report.ModelElementReference;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

import com.google.common.base.Objects;

/**
 * Compares system models against a base system model. E.g., can be used to determine the
 * differences of an old system model and the new one.
 *
 * @author Henning Schulz
 *
 */
public class ApplicationChangeDetector {

	private final Application newSystemModel;

	private final ApplicationChangeReportBuilder reportBuilder;

	/**
	 * Creates an instance with the current system model as base.
	 *
	 * @param newSystemModel
	 *            The current system model.
	 */
	public ApplicationChangeDetector(Application newSystemModel) {
		this(newSystemModel, EnumSet.noneOf(ApplicationChangeType.class));
	}

	public ApplicationChangeDetector(Application newSystemModel, EnumSet<ApplicationChangeType> ignoredChangeTypes) {
		this.newSystemModel = newSystemModel;
		this.reportBuilder = new ApplicationChangeReportBuilder(ignoredChangeTypes, newSystemModel.getTimestamp());
	}

	/**
	 * Compares an old system model to the stored one and reports differences.
	 *
	 * @param oldSystemModel
	 *            An old system model.
	 */
	public void compareTo(Application oldSystemModel) {
		reportBuilder.setBeforeChange(oldSystemModel.getTimestamp());

		final Set<ModelElementReference> visited = new HashSet<>();
		IdpaByClassSearcher<Endpoint<?>> searcher = new IdpaByClassSearcher<>(Endpoint.GENERIC_TYPE, inter -> checkInterface(inter, oldSystemModel, visited));
		searcher.visit(newSystemModel);

		searcher = new IdpaByClassSearcher<>(Endpoint.GENERIC_TYPE, inter -> reportRemovedInterface(inter, visited));
		searcher.visit(oldSystemModel);
	}

	private boolean checkInterface(Endpoint<?> newInterf, Application oldSystemModel, Set<ModelElementReference> visited) {
		final Holder<Endpoint<?>> interfHolder = new Holder<>();
		IdpaByClassSearcher<Endpoint<?>> searcher = new IdpaByClassSearcher<>(Endpoint.GENERIC_TYPE, oldInterf -> {
			if (oldInterf.getId().equals(newInterf.getId())) {
				interfHolder.element = oldInterf;
			}
		});
		searcher.visit(oldSystemModel);

		ModelElementReference ref = new ModelElementReference(newInterf);

		if (interfHolder.element == null) {
			reportBuilder.addChange(new ApplicationChange(ApplicationChangeType.ENDPOINT_ADDED, ref));
		} else {
			Endpoint<?> oldInterf = interfHolder.element;

			for (String changedProperty : oldInterf.getDifferingProperties(newInterf)) {
				if (!"parameters".equals(changedProperty)) {
					reportBuilder.addChange(new ApplicationChange(ApplicationChangeType.ENDPOINT_CHANGED, ref, changedProperty));
				}
			}

			checkParameters(oldInterf, newInterf);

			visited.add(ref);
		}

		return true;
	}

	private boolean reportRemovedInterface(Endpoint<?> oldInterf, Set<ModelElementReference> visited) {
		ModelElementReference ref = new ModelElementReference(oldInterf);
		if (!visited.contains(ref)) {
			reportBuilder.addChange(new ApplicationChange(ApplicationChangeType.ENDPOINT_REMOVED, ref));
		}

		return true;
	}

	private void checkParameters(Endpoint<?> oldInterf, Endpoint<?> newInterf) {
		if (CollectionUtils.isEqualCollection(oldInterf.getParameters(), newInterf.getParameters())) {
			return;
		}

		for (Parameter param : newInterf.getParameters()) {
			ModelElementReference ref = new ModelElementReference(param);
			List<Parameter> oldParams = oldInterf.getParameters().stream().filter(p -> Objects.equal(param.getId(), p.getId())).collect(Collectors.toList());

			if (oldParams.isEmpty()) {
				reportBuilder.addChange(new ApplicationChange(ApplicationChangeType.PARAMETER_ADDED, ref));
			} else {
				for (String changedProperty : param.getDifferingProperties(oldParams.get(0))) {
					reportBuilder.addChange(new ApplicationChange(ApplicationChangeType.PARAMETER_CHANGED, ref, changedProperty));
				}
			}
		}

		for (Parameter param : oldInterf.getParameters()) {
			List<Parameter> newParams = newInterf.getParameters().stream().filter(p -> Objects.equal(param.getId(), p.getId())).collect(Collectors.toList());

			if (newParams.isEmpty()) {
				reportBuilder.addChange(new ApplicationChange(ApplicationChangeType.PARAMETER_REMOVED, new ModelElementReference(param)));
			}
		}
	}

	/**
	 * Gets a report based on the evaluations done before.
	 *
	 * @return The report.
	 */
	public ApplicationChangeReport getReport() {
		return reportBuilder.buildReport();
	}

	private static class Holder<T> {
		T element;
	}
}
