package org.continuity.system.model.entities;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.commons.format.CommonFormats;

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
public class SystemChangeReport {

	@JsonInclude(Include.NON_NULL)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = CommonFormats.DATE_FORMAT_PATTERN)
	private Date beforeChange;
	@JsonInclude(Include.NON_NULL)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = CommonFormats.DATE_FORMAT_PATTERN)
	private Date afterChange;

	@JsonProperty("system-changes")
	@JsonInclude(Include.NON_EMPTY)
	private Set<SystemChange> systemChanges;

	@JsonProperty("ignored-system-changes")
	@JsonInclude(Include.NON_EMPTY)
	private Set<SystemChange> ignoredSystemChanges;

	/**
	 * Sets the beforeChange date to 0.
	 *
	 * @param changes
	 * @param afterChange
	 */
	public SystemChangeReport(Set<SystemChange> changes, Date afterChange) {
		this(changes, Collections.emptySet(), new Date(0), afterChange);
	}

	public SystemChangeReport(Set<SystemChange> changes, Date beforeChange, Date afterChange) {
		this(changes, Collections.emptySet(), beforeChange, afterChange);
	}

	public SystemChangeReport(Set<SystemChange> changes, Set<SystemChange> ignoredChanges, Date beforeChange, Date afterChange) {
		this.systemChanges = changes;
		this.ignoredSystemChanges = ignoredChanges;
		this.beforeChange = beforeChange;
		this.afterChange = afterChange;
	}

	/**
	 * Default constructor.
	 */
	public SystemChangeReport() {
	}

	/**
	 * Creates an empty report not holding any changes.
	 *
	 * @return An empty report.
	 */
	public static SystemChangeReport empty(Date afterChange) {
		return new SystemChangeReport(Collections.emptySet(), afterChange);
	}

	/**
	 * Creates a report holding all interfaces of a system model as changes of type
	 * {@link SystemChangeType#INTERFACE_ADDED}.
	 *
	 * @param system
	 *            The system whose interfaces should be added.
	 * @return A report holding all interfaces to be added.
	 */
	public static SystemChangeReport allOf(SystemModel system) {
		Set<SystemChange> changes = new HashSet<>();

		for (ServiceInterface<?> interf : system.getInterfaces()) {
			changes.add(new SystemChange(SystemChangeType.INTERFACE_ADDED, new ModelElementReference(interf)));
		}

		return new SystemChangeReport(changes, system.getTimestamp());
	}


	/**
	 * Gets {@link #systemChanges}.
	 *
	 * @return {@link #systemChanges}
	 */
	public Set<SystemChange> getSystemChanges() {
		return this.systemChanges;
	}

	/**
	 * Sets {@link #systemChanges}.
	 *
	 * @param systemChanges
	 *            New value for {@link #systemChanges}
	 */
	public void setSystemChanges(Set<SystemChange> systemChanges) {
		this.systemChanges = systemChanges;
	}

	/**
	 * Gets {@link #ignoredSystemChanges}.
	 *
	 * @return {@link #ignoredSystemChanges}
	 */
	public Set<SystemChange> getIgnoredSystemChanges() {
		return this.ignoredSystemChanges;
	}

	/**
	 * Sets {@link #ignoredSystemChanges}.
	 *
	 * @param ignoredSystemChanges
	 *            New value for {@link #ignoredSystemChanges}
	 */
	public void setIgnoredSystemChanges(Set<SystemChange> ignoredSystemChanges) {
		this.ignoredSystemChanges = ignoredSystemChanges;
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
		return !systemChanges.isEmpty();
	}

	public Stream<SystemChange> stream() {
		return systemChanges.stream();
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
