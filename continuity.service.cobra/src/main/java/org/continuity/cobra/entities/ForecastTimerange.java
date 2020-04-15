package org.continuity.cobra.entities;

import java.time.ZoneId;

import org.continuity.lctl.utils.DateUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ForecastTimerange {

	private long from;

	private long to;

	@JsonIgnore
	private final ZoneId timezone;

	public ForecastTimerange(long from, long to, ZoneId timezone) {
		this.from = from;
		this.to = to;
		this.timezone = timezone;
	}

	public long getFrom() {
		return from;
	}

	public void setFrom(long from) {
		this.from = from;
	}

	public long getTo() {
		return to;
	}

	public void setTo(long to) {
		this.to = to;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(formatDate(from)).append(" - ").append(formatDate(to)).toString();
	}

	private String formatDate(long timestamp) {
		if (timezone == null) {
			return Long.toString(timestamp);
		} else {
			return DateUtils.fromEpochMillis(timestamp, timezone).toString();
		}
	}

}
