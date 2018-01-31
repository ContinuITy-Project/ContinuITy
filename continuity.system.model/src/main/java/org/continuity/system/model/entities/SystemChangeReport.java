package org.continuity.system.model.entities;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz
 *
 */
public class SystemChangeReport {

	// Store dates

	@JsonIgnore
	private Set<SystemChange> changeSet;
	private Map<ModelElementReference, Set<SystemChange>> changes;

	@JsonIgnore
	private Set<SystemChange> ignoredChangeSet;
	@JsonInclude(value = Include.CUSTOM, valueFilter = IgnoredChangesFilter.class)
	private Map<ModelElementReference, Set<SystemChange>> ignoredChanges;

	public SystemChangeReport(Set<SystemChange> changes) {
		this(changes, Collections.emptySet());
	}

	public SystemChangeReport(Set<SystemChange> changes, Set<SystemChange> ignoredChanges) {
		this.changeSet = changes;
		this.changes = Collections.singletonMap(new ModelElementReference("", "System changes"), changeSet);

		this.ignoredChangeSet = ignoredChanges;
		this.ignoredChanges = Collections.singletonMap(new ModelElementReference("", "System changes"), ignoredChangeSet);
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
	public static SystemChangeReport empty() {
		return new SystemChangeReport(Collections.emptySet());
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

		return new SystemChangeReport(changes);
	}

	/**
	 * Gets {@link #changes}.
	 *
	 * @return {@link #changes}
	 */
	public Map<ModelElementReference, Set<SystemChange>> getChanges() {
		return this.changes;
	}

	/**
	 * Sets {@link #changes}.
	 *
	 * @param changes
	 *            New value for {@link #changes}
	 */
	public void setChanges(Map<ModelElementReference, Set<SystemChange>> changes) {
		this.changes = changes;
	}

	/**
	 * Sets {@link #ignoredChanges}.
	 *
	 * @param ignoredChanges
	 *            New value for {@link #ignoredChanges}
	 */
	public void setIgnoredChanges(Map<ModelElementReference, Set<SystemChange>> ignoredChanges) {
		this.ignoredChanges = ignoredChanges;
	}

	/**
	 * Gets {@link #ignoredChanges}.
	 *
	 * @return {@link #ignoredChanges}
	 */
	public Map<ModelElementReference, Set<SystemChange>> getIgnoredChanges() {
		return this.ignoredChanges;
	}

	@JsonIgnore
	public boolean changed() {
		return !changeSet.isEmpty();
	}

	public Stream<Entry<ModelElementReference, Set<SystemChange>>> stream() {
		return changes.entrySet().stream();
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

	public static class IgnoredChangesFilter {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Map)) {
				return false;
			}

			Map<?, ?> map = (Map<?, ?>) obj;
			ModelElementReference key = new ModelElementReference("", "System changes");
			if ((map.size() == 1) && map.containsKey(key)) {
				if (map.get(key) instanceof Set) {
					return ((Set<?>) map.get(key)).isEmpty();
				}
			}

			return false;
		}

	}

}
