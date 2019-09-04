package org.continuity.cobra.entities;

import org.continuity.dsl.timeseries.ContextRecord;
import org.continuity.dsl.timeseries.IntensityRecord;

public class TimedContextRecord extends ContextRecord {

	private long timestamp;

	public static TimedContextRecord fromIntensity(IntensityRecord intensity) {
		if (intensity.getContext() == null) {
			return null;
		}

		TimedContextRecord context = new TimedContextRecord();

		context.setBoolean(intensity.getContext().getBoolean());
		context.setNumeric(intensity.getContext().getNumeric());
		context.setString(intensity.getContext().getString());

		context.setTimestamp(intensity.getTimestamp());

		return context;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
