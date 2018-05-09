package org.continuity.idpa.application.changes;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.continuity.idpa.application.entities.ApplicationChange;
import org.continuity.idpa.application.entities.ApplicationChangeReport;
import org.continuity.idpa.application.entities.ApplicationChangeType;

/**
 * @author Henning Schulz
 *
 */
public class SystemChangeReportBuilder {

	private Date beforeChange;
	private Date afterChange;

	private final Set<ApplicationChange> changes = new HashSet<>();

	private final Set<ApplicationChange> ignoredChanges = new HashSet<>();

	private final EnumSet<ApplicationChangeType> ignoredChangeTypes;

	public SystemChangeReportBuilder(EnumSet<ApplicationChangeType> ignoredChangeTypes, Date beforeChange, Date afterChange) {
		this.ignoredChangeTypes = ignoredChangeTypes;
		this.beforeChange = beforeChange;
		this.afterChange = afterChange;
	}

	public SystemChangeReportBuilder(EnumSet<ApplicationChangeType> ignoredChangeTypes, Date afterChange) {
		this(ignoredChangeTypes, new Date(0), afterChange);
	}

	public SystemChangeReportBuilder(Date beforeChange, Date afterChange) {
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
	public void setBeforeChange(Date beforeChange) {
		this.beforeChange = beforeChange;
	}

	public ApplicationChangeReport buildReport() {
		return new ApplicationChangeReport(changes, ignoredChanges, beforeChange, afterChange);
	}

}
