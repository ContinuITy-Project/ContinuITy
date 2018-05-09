package org.continuity.idpa.application.entities;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.continuity.commons.format.CommonFormats;
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
public class ApplicationChangeReport {

	@JsonInclude(Include.NON_NULL)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = CommonFormats.DATE_FORMAT_PATTERN)
	private Date beforeChange;
	@JsonInclude(Include.NON_NULL)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = CommonFormats.DATE_FORMAT_PATTERN)
	private Date afterChange;

	@JsonProperty("application-changes")
	@JsonInclude(Include.NON_EMPTY)
	private Set<ApplicationChange> applicationChanges;

	@JsonProperty("ignored-application-changes")
	@JsonInclude(Include.NON_EMPTY)
	private Set<ApplicationChange> ignoredApplicationChanges;

	/**
	 * Sets the beforeChange date to 0.
	 *
	 * @param changes
	 * @param afterChange
	 */
	public ApplicationChangeReport(Set<ApplicationChange> changes, Date afterChange) {
		this(changes, Collections.emptySet(), new Date(0), afterChange);
	}

	public ApplicationChangeReport(Set<ApplicationChange> changes, Date beforeChange, Date afterChange) {
		this(changes, Collections.emptySet(), beforeChange, afterChange);
	}

	public ApplicationChangeReport(Set<ApplicationChange> changes, Set<ApplicationChange> ignoredChanges, Date beforeChange, Date afterChange) {
		this.applicationChanges = changes;
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
	public static ApplicationChangeReport empty(Date afterChange) {
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

		return new ApplicationChangeReport(changes, application.getTimestamp());
	}


	/**
	 * Gets {@link #applicationChanges}.
	 *
	 * @return {@link #applicationChanges}
	 */
	public Set<ApplicationChange> getApplicationChanges() {
		return this.applicationChanges;
	}

	/**
	 * Sets {@link #applicationChanges}.
	 *
	 * @param applicationChanges
	 *            New value for {@link #applicationChanges}
	 */
	public void setApplicationChanges(Set<ApplicationChange> applicationChanges) {
		this.applicationChanges = applicationChanges;
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
	public Date getBeforeChange() {
		return this.beforeChange;
	}

	/**
	 * Sets {@link #beforeChange}.
	 *
	 * @param beforeChange
	 *            New value for {@link #beforeChange}
	 */
	public void setBeforeChange(Date beforeChange) {
		this.beforeChange = beforeChange;
	}

	/**
	 * Gets {@link #afterChange}.
	 *
	 * @return {@link #afterChange}
	 */
	public Date getAfterChange() {
		return this.afterChange;
	}

	/**
	 * Sets {@link #afterChange}.
	 *
	 * @param afterChange
	 *            New value for {@link #afterChange}
	 */
	public void setAfterChange(Date afterChange) {
		this.afterChange = afterChange;
	}

	@JsonIgnore
	public boolean changed() {
		return !applicationChanges.isEmpty();
	}

	public Stream<ApplicationChange> stream() {
		return applicationChanges.stream();
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
