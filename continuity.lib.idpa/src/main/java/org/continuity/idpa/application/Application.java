/**
 */
package org.continuity.idpa.application;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.continuity.idpa.AbstractIdpaElement;
import org.continuity.idpa.Version;
import org.continuity.idpa.VersionOrTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents an application consisting of endpoints that can be called.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "version", "timestamp", "endpoints" })
public class Application extends AbstractIdpaElement {

	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ss-SSSX";

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
	@JsonInclude(Include.NON_NULL)
	private Date timestamp;

	@JsonInclude(Include.NON_NULL)
	private Version version;

	private List<Endpoint<?>> endpoints;

	/**
	 * Returns the endpoint representations of the represented system.
	 *
	 * @return the endpoint representations of the represented system
	 */
	public List<Endpoint<?>> getEndpoints() {
		if (endpoints == null) {
			endpoints = new ArrayList<>();
		}
		return endpoints;
	}

	/**
	 * Sets the endpoint representations of the represented system.
	 *
	 * @param endpoints
	 *            The endpoint representations of the represented system.
	 */
	public void setEndpoints(List<Endpoint<?>> endpoints) {
		this.endpoints = endpoints;
	}

	/**
	 * Gets the date at which the system is represented.
	 *
	 * @return The timestamp.
	 */
	public Date getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Sets the date at which the system is represented. <br>
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
	 * Gets the version at which the system is represented.
	 *
	 * @return
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Sets the version at which the system is represented. <br>
	 * <b>Resets a potentially stored timestamp, as they cannot be stored simultaneously!</b>
	 *
	 * @param version
	 */
	public void setVersion(Version version) {
		this.version = version;
		this.timestamp = null;
	}

	/**
	 * Gets a {@link VersionOrTimestamp} object representing the stored version or timestamp.
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

	public void addEndpoint(Endpoint<?> endpoint) {
		getEndpoints().add(endpoint);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (id: ");
		result.append(getId());
		result.append(')');
		return result.toString();
	}

}
