package org.continuity.system.annotation.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.continuity.annotation.dsl.ann.Input;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.ParameterAnnotation;
import org.continuity.annotation.dsl.ann.RegExExtraction;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;
import org.continuity.system.annotation.entities.AnnotationValidityReport;
import org.continuity.system.annotation.entities.AnnotationViolation;
import org.continuity.system.annotation.entities.AnnotationViolationType;
import org.continuity.system.annotation.entities.ModelElementReference;

/**
 * Compares system models and annotations against a base system model. E.g., can be used to
 * determine the differences of an old system model an the new one or to compare an annotation
 * against the new system model.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationValidityChecker {

	private final SystemModel newSystemModel;

	private final AnnotationValidationReportBuilder reportBuilder = new AnnotationValidationReportBuilder();

	/**
	 * Creates an instance with the current system model as base.
	 *
	 * @param newSystemModel
	 *            The current system model.
	 */
	public AnnotationValidityChecker(SystemModel newSystemModel) {
		this.newSystemModel = newSystemModel;
	}

	/**
	 * Compares an old system model to the stored one and reports differences.
	 *
	 * @param oldSystemModel
	 *            An old system model.
	 */
	public void compareToOldSystemModel(SystemModel oldSystemModel) {
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
			reportBuilder.addViolation(new AnnotationViolation(AnnotationViolationType.INTERFACE_ADDED, ref));
		} else {
			ServiceInterface<?> oldInterf = interfHolder.element;

			if (!oldInterf.equals(newInterf)) {
				reportBuilder.addViolation(new AnnotationViolation(AnnotationViolationType.INTERFACE_CHANGED, ref));
			}

			checkParameters(oldInterf, newInterf);

			visited.add(ref);
		}

		return true;
	}

	private boolean reportRemovedInterface(ServiceInterface<?> oldInterf, Set<ModelElementReference> visited) {
		ModelElementReference ref = new ModelElementReference(oldInterf);
		if (!visited.contains(ref)) {
			reportBuilder.addViolation(new AnnotationViolation(AnnotationViolationType.INTERFACE_REMOVED, ref));
		}

		return true;
	}

	private void checkParameters(ServiceInterface<?> oldInterf, ServiceInterface<?> newInterf) {
		if (CollectionUtils.isEqualCollection(oldInterf.getParameters(), newInterf.getParameters())) {
			return;
		}

		for (Parameter param : newInterf.getParameters()) {
			if (!oldInterf.getParameters().contains(param)) {
				reportBuilder.addViolation(new AnnotationViolation(AnnotationViolationType.PARAMETER_ADDED, new ModelElementReference(param)));
			}
		}

		for (Parameter param : oldInterf.getParameters()) {
			if (!newInterf.getParameters().contains(param)) {
				reportBuilder.addViolation(new AnnotationViolation(AnnotationViolationType.PARAMETER_REMOVED, new ModelElementReference(param)));
			}
		}
	}

	/**
	 * Compares an annotation to the stored system model and reports broken references.
	 *
	 * @param annotation
	 *            An annotation.
	 */
	public void checkAnnotation(SystemAnnotation annotation) {
		checkAnnotationInternally(annotation);
		checkAnnotationForExternalReferences(annotation);
	}

	private void checkAnnotationInternally(SystemAnnotation annotation) {
		ContinuityByClassSearcher<ParameterAnnotation> paramSearcher = new ContinuityByClassSearcher<>(ParameterAnnotation.class, ann -> {
			List<Input> inputs = annotation.getInputs();
			boolean inputNotPresent = inputs.stream().map(Input::getId).filter(id -> Objects.equals(id, ann.getInput().getId())).collect(Collectors.toList()).isEmpty();

			if (inputNotPresent) {
				ModelElementReference inputRef = new ModelElementReference(ann.getInput());
				ModelElementReference annRef = new ModelElementReference(ann);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEGAL_INTERNAL_REFERENCE, inputRef));
			}
		});

		paramSearcher.visit(annotation);
	}

	private void checkAnnotationForExternalReferences(SystemAnnotation annotation) {
		ContinuityByClassSearcher<InterfaceAnnotation> interfaceSearcher = new ContinuityByClassSearcher<>(InterfaceAnnotation.class, ann -> {
			ServiceInterface<?> interf = ann.getAnnotatedInterface().resolve(newSystemModel);

			if (interf == null) {
				ModelElementReference interfRef = new ModelElementReference(ann.getAnnotatedInterface());
				ModelElementReference annRef = new ModelElementReference(ann);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEAL_INTERFACE_REFERENCE, interfRef));
			}

			reportBuilder.resolveInterfaceAnnotation(ann);
		});

		interfaceSearcher.visit(annotation);

		ContinuityByClassSearcher<ParameterAnnotation> paramSearcher = new ContinuityByClassSearcher<>(ParameterAnnotation.class, ann -> {
			Parameter param = ann.getAnnotatedParameter().resolve(newSystemModel);

			if (param == null) {
				ModelElementReference paramRef = new ModelElementReference(ann.getAnnotatedParameter());
				ModelElementReference annRef = new ModelElementReference(ann);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEGAL_PARAMETER_REFERENCE, paramRef));
			}

			reportBuilder.resolveParameterAnnotation(ann);
		});

		paramSearcher.visit(annotation);

		ContinuityByClassSearcher<RegExExtraction> extractionSearcher = new ContinuityByClassSearcher<>(RegExExtraction.class, extraction -> {
			ServiceInterface<?> interf = extraction.getFrom().resolve(newSystemModel);

			if (interf == null) {
				ModelElementReference interfRef = new ModelElementReference(extraction.getFrom());
				ModelElementReference annRef = new ModelElementReference(extraction);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEAL_INTERFACE_REFERENCE, interfRef));
			}
		});

		extractionSearcher.visit(annotation);
	}

	/**
	 * Gets a report based on the evaluations done before.
	 *
	 * @return The report.
	 */
	public AnnotationValidityReport getReport() {
		return reportBuilder.buildReport();
	}

	private static class Holder<T> {
		T element;
	}
}
