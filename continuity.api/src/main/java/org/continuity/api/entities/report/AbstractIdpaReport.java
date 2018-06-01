package org.continuity.api.entities.report;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractIdpaReport {

	@JsonProperty("application-changes")
	@JsonInclude(Include.NON_EMPTY)
	private Set<ApplicationChange> applicationChanges;

	public AbstractIdpaReport() {
	}

	public AbstractIdpaReport(Set<ApplicationChange> changes) {
		this.applicationChanges = changes;
	}

	/**
	 * Gets {@link #applicationChanges}.
	 *
	 * @return {@link #applicationChanges}
	 */
	public Set<ApplicationChange> getApplicationChanges() {
		if (applicationChanges == null) {
			applicationChanges = new HashSet<>();
		}

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

	@JsonIgnore
	public boolean changed() {
		return !applicationChanges.isEmpty();
	}

	public Stream<ApplicationChange> stream() {
		return applicationChanges.stream();
	}

}
