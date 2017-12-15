package org.continuity.workload.annotation.entities;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationValidityReport {

	@JsonValue
	private final Map<ModelElementReference, Set<AnnotationViolation>> violations;

	@JsonCreator
	public AnnotationValidityReport(Map<ModelElementReference, Set<AnnotationViolation>> violations) {
		this.violations = violations;
	}

	/**
	 * Gets {@link #violations}.
	 *
	 * @return {@link #violations}
	 */
	public Map<ModelElementReference, Set<AnnotationViolation>> getViolations() {
		return this.violations;
	}

	@JsonIgnore
	public boolean isOk() {
		return violations.isEmpty();
	}

	@JsonIgnore
	public boolean isBreaking() {
		return violations.values().stream().flatMap(Set::stream).reduce(false, (b, v) -> b || v.isBreaking(), Boolean::logicalOr);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return super.toString() + " [ERROR during serialization!]";
		}
	}

}
