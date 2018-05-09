package org.continuity.idpa.annotation.validation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.entities.AnnotationValidityReport;
import org.continuity.idpa.annotation.entities.AnnotationViolation;
import org.continuity.idpa.annotation.entities.ModelElementReference;
import org.continuity.idpa.application.Parameter;

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
		violationsPerReferenced.put(violation.getChangedElement(), violation);
	}

	public void addViolations(Set<AnnotationViolation> violations) {
		for (AnnotationViolation v : violations) {
			addViolation(v);
		}
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
	 * {@link WeakReference#resolve(org.continuity.idpa.ContinuityModelElement)}.
	 *
	 * @param annotation
	 *            The interface annotation.
	 */
	public void resolveInterfaceAnnotation(EndpointAnnotation annotation) {
		AnnotationViolation violation = violationsPerReferenced.get(new ModelElementReference(annotation.getAnnotatedEndpoint()));
		ModelElementReference ref = new ModelElementReference(annotation);

		if (violation != null) {
			getViolationSet(ref).add(violation);
		}

		if (annotation.getAnnotatedEndpoint().isResolved()) {
			for (Parameter parameter : annotation.getAnnotatedEndpoint().getReferred().getParameters()) {
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

		Set<AnnotationViolation> systemChanges;

		if (violationsPerReferenced.isEmpty()) {
			systemChanges = Collections.emptySet();
		} else {
			systemChanges = new HashSet<>();
			systemChanges.addAll(violationsPerReferenced.values());
		}

		return new AnnotationValidityReport(systemChanges, report);
	}

}
