package org.continuity.workload.annotation.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.continuity.annotation.dsl.WeakReference;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.ParameterAnnotation;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.workload.annotation.entities.AnnotationValidityReport;
import org.continuity.workload.annotation.entities.AnnotationViolation;
import org.continuity.workload.annotation.entities.ModelElementReference;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationValidationReportBuilder {

	/**
	 * Affected annotation --> Set of violations
	 */
	private final Map<ModelElementReference, Set<AnnotationViolation>> violations = new HashMap<>();

	/**
	 * Referenced --> Violation
	 */
	private final Map<ModelElementReference, AnnotationViolation> violationsPerReferenced = new HashMap<>();

	public void addViolation(AnnotationViolation violation) {
		violationsPerReferenced.put(violation.getReferenced(), violation);
	}

	public void addViolation(ModelElementReference affected, AnnotationViolation violation) {
		getViolationSet(affected).add(violation);
	}

	public void resolveParameterAnnotation(ParameterAnnotation annotation) {
		AnnotationViolation violation = violationsPerReferenced.get(new ModelElementReference(annotation.getAnnotatedParameter()));

		if (violation != null) {
			ModelElementReference ref = new ModelElementReference(annotation.getAnnotatedParameter());
			getViolationSet(ref).add(violation);
		}
	}

	/**
	 * Resolves the violations affecting the specified interface annotation. <br>
	 * <b>Note:</b> The annotated interface has to be resolved (call
	 * {@link WeakReference#resolve(org.continuity.annotation.dsl.ContinuityModelElement)}.
	 *
	 * @param annotation
	 *            The interface annotation.
	 */
	public void resolveInterfaceAnnotation(InterfaceAnnotation annotation) {
		AnnotationViolation violation = violationsPerReferenced.get(new ModelElementReference(annotation.getAnnotatedInterface()));
		ModelElementReference ref = new ModelElementReference(annotation);

		if (violation != null) {
			getViolationSet(ref).add(violation);
		}

		if (annotation.getAnnotatedInterface().isResolved()) {
			for (Parameter parameter : annotation.getAnnotatedInterface().getReferred().getParameters()) {
				violation = violationsPerReferenced.get(new ModelElementReference(parameter));

				if (violation != null) {
					getViolationSet(ref).add(violation);
				}
			}
		}
	}

	private Set<AnnotationViolation> getViolationSet(ModelElementReference reference) {
		Set<AnnotationViolation> violationSet = violations.get(reference);

		if (violationSet == null) {
			violationSet = new HashSet<>();
			violations.put(reference, violationSet);
		}

		return violationSet;
	}

	public AnnotationValidityReport buildReport() {
		Map<ModelElementReference, Set<AnnotationViolation>> report = new HashMap<>();
		report.putAll(violations);

		if (!violationsPerReferenced.isEmpty()) {
			Set<AnnotationViolation> violationSet = new HashSet<>();
			violationSet.addAll(violationsPerReferenced.values());
			report.put(new ModelElementReference("", "System changes"), violationSet);
		}

		return new AnnotationValidityReport(report);
	}

}
