package org.continuity.idpa.annotation.entities;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.continuity.idpa.annotation.entities.ModelElementReference.RefKeyDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Henning Schulz
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationValidityReport {

	@JsonProperty("application-changes")
	private Set<AnnotationViolation> applicationChanges;

	@JsonDeserialize(keyUsing = RefKeyDeserializer.class)
	private Map<ModelElementReference, Set<AnnotationViolation>> violations;

	@JsonProperty("violations-before-fix")
	@JsonInclude(Include.NON_NULL)
	@JsonDeserialize(keyUsing = RefKeyDeserializer.class)
	private Map<ModelElementReference, Set<AnnotationViolation>> violationsBeforeFix;

	public AnnotationValidityReport(Set<AnnotationViolation> applicationChanges, Map<ModelElementReference, Set<AnnotationViolation>> violations) {
		this.applicationChanges = applicationChanges;
		this.violations = violations;
	}

	/**
	 * Default constructor.
	 */
	public AnnotationValidityReport() {
	}

	/**
	 * Gets {@link #applicationChanges}.
	 *
	 * @return {@link #applicationChanges}
	 */
	public Set<AnnotationViolation> getApplicationChanges() {
		return this.applicationChanges;
	}

	/**
	 * Sets {@link #applicationChanges}.
	 *
	 * @param applicationChanges
	 *            New value for {@link #applicationChanges}
	 */
	public void setApplicationChanges(Set<AnnotationViolation> applicationChanges) {
		this.applicationChanges = applicationChanges;
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
		return applicationChanges.isEmpty() && violations.isEmpty();
	}

	@JsonIgnore
	public boolean isBreaking() {
		boolean breaking = violations.values().stream().flatMap(Set::stream).reduce(false, (b, v) -> b || v.isBreaking(), Boolean::logicalOr);
		return breaking || applicationChanges.stream().reduce(false, (b, v) -> b || v.isBreaking(), Boolean::logicalOr);
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

	/**
	 * @return
	 */
	public static AnnotationValidityReport empty() {
		return new AnnotationValidityReport(Collections.emptySet(), Collections.emptyMap());
	}

}
