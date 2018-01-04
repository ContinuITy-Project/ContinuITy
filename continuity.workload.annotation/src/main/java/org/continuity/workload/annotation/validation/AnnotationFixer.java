package org.continuity.workload.annotation.validation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.continuity.annotation.dsl.ann.ExtractedInput;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.RegExExtraction;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.visitor.ContinuityByClassSearcher;
import org.continuity.workload.annotation.entities.AnnotationValidityReport;
import org.continuity.workload.annotation.entities.AnnotationViolation;
import org.continuity.workload.annotation.entities.AnnotationViolationType;
import org.continuity.workload.annotation.entities.ModelElementReference;

/**
 * Fixes broken annotations.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationFixer {

	/**
	 * Tries to fix the passed annotation based on the passed report. <br>
	 * <b>Note: The returned annotation can still be broken!</b>
	 *
	 * @param brokenAnnotation
	 *            The annotation to be fixed.
	 * @param report
	 *            The report holding the violations.
	 * @return An annotation that might be fixed.
	 */
	public SystemAnnotation createFixedAnnotation(SystemAnnotation brokenAnnotation, AnnotationValidityReport report) {
		if (!report.isBreaking()) {
			return brokenAnnotation;
		}

		return removeUnknownInterfaceReferences(brokenAnnotation, report);
	}

	private SystemAnnotation removeUnknownInterfaceReferences(SystemAnnotation brokenAnnotation, AnnotationValidityReport report) {
		Set<String> removedInterfaces = new HashSet<>();

		for (Map.Entry<ModelElementReference, Set<AnnotationViolation>> violationEntry : report.getViolations().entrySet()) {

			for (AnnotationViolation violation : violationEntry.getValue()) {
				if (violation.getType() == AnnotationViolationType.INTERFACE_REMOVED) {
					removedInterfaces.add(violation.getReferenced().getId());
				}
			}
		}

		SystemAnnotation fixedAnnotation = new SystemAnnotation();
		fixedAnnotation.setId(brokenAnnotation.getId() + "-wouir");
		fixedAnnotation.setInputs(brokenAnnotation.getInputs().stream().filter(input -> !(input instanceof ExtractedInput)).collect(Collectors.toList()));
		fixedAnnotation.setOverrides(brokenAnnotation.getOverrides());

		ContinuityByClassSearcher<InterfaceAnnotation> interfSearcher = new ContinuityByClassSearcher<>(InterfaceAnnotation.class,
				ann -> addInterfaceAnnotationIfNotRemoved(ann, fixedAnnotation, removedInterfaces));
		interfSearcher.visit(brokenAnnotation);

		ContinuityByClassSearcher<ExtractedInput> inputSearcher = new ContinuityByClassSearcher<>(ExtractedInput.class,
				input -> addExtracedInputIfNoReferenceRemoved(input, fixedAnnotation, removedInterfaces));
		inputSearcher.visit(brokenAnnotation);

		return fixedAnnotation;
	}

	private void addInterfaceAnnotationIfNotRemoved(InterfaceAnnotation annotation, SystemAnnotation fixedAnnotation, Set<String> removedInterfaces) {
		if (!removedInterfaces.contains(annotation.getAnnotatedInterface().getId())) {
			fixedAnnotation.getInterfaceAnnotations().add(annotation);
		}
	}

	private void addExtracedInputIfNoReferenceRemoved(ExtractedInput input, SystemAnnotation fixedAnnotation, Set<String> removedInterfaces) {
		boolean broken = false;

		for (RegExExtraction extraction : input.getExtractions()) {
			if (removedInterfaces.contains(extraction.getFrom().getId())) {
				broken = true;
				break;
			}
		}

		if (!broken) {
			fixedAnnotation.addInput(input);
		}
	}

}
