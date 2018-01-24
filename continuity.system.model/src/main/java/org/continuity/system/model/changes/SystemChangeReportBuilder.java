package org.continuity.system.model.changes;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.continuity.system.model.entities.SystemChange;
import org.continuity.system.model.entities.SystemChangeReport;
import org.continuity.system.model.entities.SystemChangeType;

/**
 * @author Henning Schulz
 *
 */
public class SystemChangeReportBuilder {

	private final Set<SystemChange> changes = new HashSet<>();

	private final Set<SystemChange> ignoredChanges = new HashSet<>();

	private final EnumSet<SystemChangeType> ignoredChangeTypes;

	public SystemChangeReportBuilder(EnumSet<SystemChangeType> ignoredChangeTypes) {
		this.ignoredChangeTypes = ignoredChangeTypes;
	}

	public SystemChangeReportBuilder() {
		this(EnumSet.noneOf(SystemChangeType.class));
	}

	public void addChange(SystemChange violation) {
		if (!ignoredChangeTypes.contains(violation.getType())) {
			changes.add(violation);
		} else {
			ignoredChanges.add(violation);
		}
	}

	public SystemChangeReport buildReport() {
		return new SystemChangeReport(changes, ignoredChanges);
	}

}
