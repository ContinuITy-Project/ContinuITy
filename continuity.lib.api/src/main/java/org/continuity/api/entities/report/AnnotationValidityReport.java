package org.continuity.api.entities.report;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.continuity.api.entities.report.ModelElementReference.RefKeyDeserializer;

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
public class AnnotationValidityReport extends AbstractIdpaReport {

	@JsonDeserialize(keyUsing = RefKeyDeserializer.class)
	private Map<ModelElementReference, Set<AnnotationViolation>> violations;

	@JsonProperty("violations-before-fix")
	@JsonInclude(Include.NON_NULL)
	@JsonDeserialize(keyUsing = RefKeyDeserializer.class)
	private Map<ModelElementReference, Set<AnnotationViolation>> violationsBeforeFix;

	public AnnotationValidityReport(Set<ApplicationChange> applicationChanges, Map<ModelElementReference, Set<AnnotationViolation>> violations) {
		super(applicationChanges);

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
		return getApplicationChanges().isEmpty() && violations.isEmpty();
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

	/**
	 * @return
	 */
	public static AnnotationValidityReport empty() {
		return new AnnotationValidityReport(Collections.emptySet(), Collections.emptyMap());
	}

}
