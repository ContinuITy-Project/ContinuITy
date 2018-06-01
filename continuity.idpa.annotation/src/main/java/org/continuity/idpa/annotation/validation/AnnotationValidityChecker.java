package org.continuity.idpa.annotation.validation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.api.entities.report.AnnotationViolation;
import org.continuity.api.entities.report.AnnotationViolationType;
import org.continuity.api.entities.report.ModelElementReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.visitor.IdpaByClassSearcher;

/**
 * Compares application models and annotations against a base application model. E.g., can be used
 * to determine the differences of an old application model an the new one or to compare an
 * annotation against the new application model.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationValidityChecker {

	private final Application newApplication;

	private final AnnotationValidityReportBuilder reportBuilder = new AnnotationValidityReportBuilder();

	/**
	 * Creates an instance with the current application model as base.
	 *
	 * @param newApplication
	 *            The current application model.
	 */
	public AnnotationValidityChecker(Application newApplication) {
		this.newApplication = newApplication;
	}

	public void registerApplicationChanges(AnnotationValidityReport applicationChangeReport) {
		reportBuilder.addApplicationChanges(applicationChangeReport.getApplicationChanges());
	}

	/**
	 * Compares an annotation to the stored application model and reports broken references.
	 *
	 * @param annotation
	 *            An annotation.
	 */
	public void checkAnnotation(ApplicationAnnotation annotation) {
		checkAnnotationInternally(annotation);
		checkAnnotationForExternalReferences(annotation);
	}

	private void checkAnnotationInternally(ApplicationAnnotation annotation) {
		IdpaByClassSearcher<ParameterAnnotation> paramSearcher = new IdpaByClassSearcher<>(ParameterAnnotation.class, ann -> {
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

	private void checkAnnotationForExternalReferences(ApplicationAnnotation annotation) {
		IdpaByClassSearcher<EndpointAnnotation> interfaceSearcher = new IdpaByClassSearcher<>(EndpointAnnotation.class, ann -> {
			Endpoint<?> interf = ann.getAnnotatedEndpoint().resolve(newApplication);

			if (interf == null) {
				ModelElementReference interfRef = new ModelElementReference(ann.getAnnotatedEndpoint());
				ModelElementReference annRef = new ModelElementReference(ann);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEGAL_ENDPOINT_REFERENCE, interfRef));
			}

			reportBuilder.resolveInterfaceAnnotation(ann);
		});

		interfaceSearcher.visit(annotation);

		IdpaByClassSearcher<ParameterAnnotation> paramSearcher = new IdpaByClassSearcher<>(ParameterAnnotation.class, ann -> {
			Parameter param = ann.getAnnotatedParameter().resolve(newApplication);

			if (param == null) {
				ModelElementReference paramRef = new ModelElementReference(ann.getAnnotatedParameter());
				ModelElementReference annRef = new ModelElementReference(ann);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEGAL_PARAMETER_REFERENCE, paramRef));
			}

			reportBuilder.resolveParameterAnnotation(ann);
		});

		paramSearcher.visit(annotation);

		IdpaByClassSearcher<RegExExtraction> extractionSearcher = new IdpaByClassSearcher<>(RegExExtraction.class, extraction -> {
			Endpoint<?> interf = extraction.getFrom().resolve(newApplication);

			if (interf == null) {
				ModelElementReference interfRef = new ModelElementReference(extraction.getFrom());
				ModelElementReference annRef = new ModelElementReference(extraction);
				reportBuilder.addViolation(annRef, new AnnotationViolation(AnnotationViolationType.ILLEGAL_ENDPOINT_REFERENCE, interfRef));
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
