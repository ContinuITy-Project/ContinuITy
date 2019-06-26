package org.continuity.commons.idpa;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.continuity.api.entities.report.ApplicationChange;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.idpa.VersionOrTimestamp;

/**
 * @author Henning Schulz
 *
 */
public class ApplicationChangeReportBuilder {

	private VersionOrTimestamp beforeChange;
	private VersionOrTimestamp afterChange;

	private final Set<ApplicationChange> changes = new HashSet<>();

	private final Set<ApplicationChange> ignoredChanges = new HashSet<>();

	private final EnumSet<ApplicationChangeType> ignoredChangeTypes;

	public ApplicationChangeReportBuilder(EnumSet<ApplicationChangeType> ignoredChangeTypes, VersionOrTimestamp beforeChange, VersionOrTimestamp afterChange) {
		this.ignoredChangeTypes = ignoredChangeTypes;
		this.beforeChange = beforeChange;
		this.afterChange = afterChange;
	}

	public ApplicationChangeReportBuilder(EnumSet<ApplicationChangeType> ignoredChangeTypes, VersionOrTimestamp afterChange) {
		this(ignoredChangeTypes, VersionOrTimestamp.MIN_VALUE, afterChange);
	}

	public ApplicationChangeReportBuilder(VersionOrTimestamp beforeChange, VersionOrTimestamp afterChange) {
		this(EnumSet.noneOf(ApplicationChangeType.class), beforeChange, afterChange);
	}

	public void addChange(ApplicationChange violation) {
		if (!ignoredChangeTypes.contains(violation.getType())) {
			changes.add(violation);
		} else {
			ignoredChanges.add(violation);
		}
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

	public ApplicationChangeReport buildReport() {
		return new ApplicationChangeReport(changes, ignoredChanges, beforeChange, afterChange);
	}

}
