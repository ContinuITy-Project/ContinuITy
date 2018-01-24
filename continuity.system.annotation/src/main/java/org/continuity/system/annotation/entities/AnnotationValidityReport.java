package org.continuity.system.annotation.entities;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationValidityReport {

	private Map<ModelElementReference, Set<AnnotationViolation>> violations;

	@JsonProperty("violations-before-fix")
	@JsonInclude(Include.NON_NULL)
	private Map<ModelElementReference, Set<AnnotationViolation>> violationsBeforeFix;

	public AnnotationValidityReport(Map<ModelElementReference, Set<AnnotationViolation>> violations) {
		this.violations = violations;
	}

	/**
	 * Default constructor.
	 */
	public AnnotationValidityReport() {
	}

	/**
	 * Gets {@link #violations}.
	 *
	 * @return {@link #violations}
	 */
	public Map<ModelElementReference, Set<AnnotationViolation>> getViolations() {
		return this.violations;
	}

	/**
	 * Sets {@link #violations}.
	 * 
	 * @param violations
	 *            New value for {@link #violations}
	 */
	public void setViolations(Map<ModelElementReference, Set<AnnotationViolation>> violations) {
		this.violations = violations;
	}

	/**
	 * Gets {@link #violationsBeforeFix}.
	 *
	 * @return {@link #violationsBeforeFix}
	 */
	public Map<ModelElementReference, Set<AnnotationViolation>> getViolationsBeforeFix() {
		return this.violationsBeforeFix;
	}

	/**
	 * Sets {@link #violationsBeforeFix}.
	 *
	 * @param violationsBeforeFix
	 *            New value for {@link #violationsBeforeFix}
	 */
	public void setViolationsBeforeFix(Map<ModelElementReference, Set<AnnotationViolation>> violationsBeforeFix) {
		this.violationsBeforeFix = violationsBeforeFix;
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
