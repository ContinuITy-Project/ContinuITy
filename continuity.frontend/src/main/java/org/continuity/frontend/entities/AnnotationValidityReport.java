package org.continuity.frontend.entities;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationValidityReport {

	private final Map<ModelElementReference, Set<AnnotationViolation>> violations;

	public AnnotationValidityReport(Map<ModelElementReference, Set<AnnotationViolation>> violations) {
		this.violations = violations;
	}

	@JsonIgnore
	public boolean isOk() {
		return violations.isEmpty();
	}

	@JsonIgnore
	public boolean isBreaking() {
		return violations.values().stream().flatMap(Set::stream).reduce(false, (b, v) -> b || v.isBreaking(), Boolean::logicalOr);
	}

}
