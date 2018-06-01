package org.continuity.idpa.annotation.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.api.entities.report.AnnotationViolation;
import org.continuity.api.entities.report.ApplicationChange;
import org.continuity.api.entities.report.ModelElementReference;
import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.application.Parameter;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationValidityReportBuilder {

	private final Set<ApplicationChange> applicationChanges = new HashSet<>();

	/**
	 * Affected annotation --> Set of violations
	 */
	private final Map<ModelElementReference, Set<AnnotationViolation>> violations = new HashMap<>();

	/**
	 * Referenced --> Violation
	 */
	private final Map<ModelElementReference, AnnotationViolation> violationsPerReferenced = new HashMap<>();

	public void addApplicationChange(ApplicationChange change) {
		applicationChanges.add(change);
	}

	public void addApplicationChanges(Set<ApplicationChange> changes) {
		applicationChanges.addAll(changes);
	}

	public void addViolation(AnnotationViolation violation) {
		violationsPerReferenced.put(violation.getAffectedElement(), violation);
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

		if (!violationsPerReferenced.isEmpty()) {
			Set<AnnotationViolation> unresolvedViolations = new HashSet<>();
			unresolvedViolations.addAll(violationsPerReferenced.values());
			violations.put(new ModelElementReference("UNRESOLVED", "?"), unresolvedViolations);
		}

		return new AnnotationValidityReport(applicationChanges, report);
	}

}
