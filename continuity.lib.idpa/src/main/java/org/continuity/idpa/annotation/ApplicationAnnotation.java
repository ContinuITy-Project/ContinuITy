/**
 */
package org.continuity.idpa.annotation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.continuity.idpa.Version;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.application.Application;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Annotation of a {@link org.continuity.idpa.application.Application} representation. Holds manual
 * changes that should be kept while changing the system itself. <br>
 *
 * An annotation consists of a collection of inputs that can be either input data or extractions
 * from the responses of called interfaces. Additionally, it holds annotations of endpoints stating
 * which inputs map to which parameters.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "version", "timestamp", "overrides", "inputs", "endpoint-annotations" })
public class ApplicationAnnotation extends OverrideableAnnotation<PropertyOverrideKey.Any> {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Application.DATE_FORMAT)
	@JsonInclude(Include.NON_NULL)
	private Date timestamp;

	@JsonInclude(Include.NON_NULL)
	private Version version;

	@JsonProperty(value = "inputs")
	private List<Input> inputs;

	@JsonProperty(value = "endpoint-annotations")
	private List<EndpointAnnotation> endpointAnnotations;

	/**
	 * Gets the date from which on the annotation is valid.
	 *
	 * @return The timestamp.
	 */
	public Date getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Sets the date from which on the annotation is valid. <br>
	 * <b>Resets a potentially stored version, as they cannot be stored simultaneously!</b>
	 *
	 * @param timestamp
	 *            The timestamp.
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
		this.version = null;
	}

	/**
	 * Gets the version from which on the annotation is valid.
	 *
	 * @return
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Sets the version from which on the annotation is valid. <br>
	 * <b>Resets a potentially stored timestamp, as they cannot be stored simultaneously!</b>
	 *
	 * @param version
	 */
	public void setVersion(Version version) {
		this.version = version;
		this.timestamp = null;
	}

	/**
	 * Gets a {@link VersionOrTimestamp} objects representing the stored version or timestamp.
	 *
	 * @see #getVersion()
	 * @see #getTimestamp()
	 *
	 * @return
	 */
	@JsonIgnore
	public VersionOrTimestamp getVersionOrTimestamp() {
		return new VersionOrTimestamp(version, timestamp);
	}

	/**
	 * Sets the timestamp or version based on a {@link VersionOrTimestamp} object.
	 *
	 * @see #setVersion()
	 * @see #setTimestamp()
	 *
	 * @return
	 */
	@JsonIgnore
	public void setVersionOrTimestamp(VersionOrTimestamp versionOrTimestamp) {
		if (versionOrTimestamp.isVersion()) {
			setVersion(versionOrTimestamp.getVersion());
		} else {
			setTimestamp(versionOrTimestamp.getTimestamp());
		}
	}

	/**
	 * Returns the inputs.
	 *
	 * @return The inputs.
	 */
	public List<Input> getInputs() {
		if (inputs == null) {
			inputs = new ArrayList<>();
		}

		return this.inputs;
	}

	/**
	 * Sets the inputs.
	 *
	 * @param inputs
	 *            The inputs.
	 */
	public void setInputs(List<Input> inputs) {
		this.inputs = inputs;
	}

	/**
	 * Adds an input.
	 *
	 * @param input
	 *            The input to be added.
	 */
	public void addInput(Input input) {
		getInputs().add(input);
	}

	/**
	 * Returns the endpoint annotations.
	 *
	 * @return {@link #endpointAnnotations}
	 */
	public List<EndpointAnnotation> getEndpointAnnotations() {
		if (endpointAnnotations == null) {
			endpointAnnotations = new ArrayList<>();
		}

		return this.endpointAnnotations;
	}

	/**
	 * Sets the endpoint annotations.
	 *
	 * @param endpointAnnotations
	 *            The endpoint annotations.
	 */
	public void setEndpointAnnotations(List<EndpointAnnotation> endpointAnnotations) {
		this.endpointAnnotations = endpointAnnotations;
	}

}
