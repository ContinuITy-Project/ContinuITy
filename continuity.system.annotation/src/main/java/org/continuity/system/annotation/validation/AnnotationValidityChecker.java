package org.continuity.system.annotation.validation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

	public void registerSystemChanges(AnnotationValidityReport systemChangeReport) {
		reportBuilder.addViolations(systemChangeReport.getSystemChanges());
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
}
