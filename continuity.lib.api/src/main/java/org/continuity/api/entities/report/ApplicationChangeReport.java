package org.continuity.api.entities.report;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.continuity.api.entities.ApiFormats;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ApplicationChangeReport extends AbstractIdpaReport {

	@JsonInclude(Include.NON_NULL)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiFormats.DATE_FORMAT_PATTERN)
	private VersionOrTimestamp beforeChange;
	@JsonInclude(Include.NON_NULL)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiFormats.DATE_FORMAT_PATTERN)
	private VersionOrTimestamp afterChange;

	@JsonProperty("ignored-application-changes")
	@JsonInclude(Include.NON_EMPTY)
	private Set<ApplicationChange> ignoredApplicationChanges;

	@JsonIgnore
	private Application updatedApplication;

	/**
	 * Sets the beforeChange date to 0.
	 *
	 * @param changes
	 * @param afterChange
	 */
	public ApplicationChangeReport(Set<ApplicationChange> changes, VersionOrTimestamp afterChange) {
		this(changes, Collections.emptySet(), VersionOrTimestamp.MIN_VALUE, afterChange);
	}

	public ApplicationChangeReport(Set<ApplicationChange> changes, VersionOrTimestamp beforeChange, VersionOrTimestamp afterChange) {
		this(changes, Collections.emptySet(), beforeChange, afterChange);
	}

	public ApplicationChangeReport(Set<ApplicationChange> changes, Set<ApplicationChange> ignoredChanges, VersionOrTimestamp beforeChange, VersionOrTimestamp afterChange) {
		super(changes);

		this.ignoredApplicationChanges = ignoredChanges;
		this.beforeChange = beforeChange;
		this.afterChange = afterChange;
	}

	/**
	 * Default constructor.
	 */
	public ApplicationChangeReport() {
	}

	/**
	 * Creates an empty report not holding any changes.
	 *
	 * @return An empty report.
	 */
	public static ApplicationChangeReport empty(VersionOrTimestamp afterChange) {
		return new ApplicationChangeReport(Collections.emptySet(), afterChange);
	}

	/**
	 * Creates a report holding all interfaces of a system model as changes of type
	 * {@link ApplicationChangeType#ENDPOINT_ADDED}.
	 *
	 * @param application
	 *            The system whose interfaces should be added.
	 * @return A report holding all interfaces to be added.
	 */
	public static ApplicationChangeReport allOf(Application application) {
		Set<ApplicationChange> changes = new HashSet<>();

		for (Endpoint<?> interf : application.getEndpoints()) {
			changes.add(new ApplicationChange(ApplicationChangeType.ENDPOINT_ADDED, new ModelElementReference(interf)));
		}

		return new ApplicationChangeReport(changes, application.getVersionOrTimestamp());
	}

	/**
	 * Gets {@link #ignoredApplicationChanges}.
	 *
	 * @return {@link #ignoredApplicationChanges}
	 */
	public Set<ApplicationChange> getIgnoredApplicationChanges() {
		return this.ignoredApplicationChanges;
	}

	/**
	 * Sets {@link #ignoredApplicationChanges}.
	 *
	 * @param ignoredSystemChanges
	 *            New value for {@link #ignoredApplicationChanges}
	 */
	public void setIgnoredApplicationChanges(Set<ApplicationChange> ignoredSystemChanges) {
		this.ignoredApplicationChanges = ignoredSystemChanges;
	}

	/**
	 * Gets {@link #beforeChange}.
	 *
	 * @return {@link #beforeChange}
	 */
	public VersionOrTimestamp getBeforeChange() {
		return this.beforeChange;
	}

	/**
	 * Sets {@link #beforeChange}.
	 *
	 * @param beforeChange
	 *            New value for {@link #beforeChange}
	 */
	public void setBeforeChange(VersionOrTimestamp beforeChange) {
		this.beforeChange = beforeChange;
	}

	/**
	 * Gets {@link #afterChange}.
	 *
	 * @return {@link #afterChange}
	 */
	public VersionOrTimestamp getAfterChange() {
		return this.afterChange;
	}

	/**
	 * Sets {@link #afterChange}.
	 *
	 * @param afterChange
	 *            New value for {@link #afterChange}
	 */
	public void setAfterChange(VersionOrTimestamp afterChange) {
		this.afterChange = afterChange;
	}

	/**
	 * Gets the updated application (only for local use; won't be sent via REST).
	 *
	 * @return
	 */
	public Application getUpdatedApplication() {
		return updatedApplication;
	}

	/**
	 * Sets the updated application (only for local use; won't be sent via REST).
	 *
	 * @param updatedApplication
	 */
	public void setUpdatedApplication(Application updatedApplication) {
		this.updatedApplication = updatedApplication;
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
