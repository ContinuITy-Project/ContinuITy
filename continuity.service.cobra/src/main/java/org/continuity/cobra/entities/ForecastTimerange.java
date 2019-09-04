package org.continuity.cobra.entities;

import java.util.Date;

public class ForecastTimerange {

	public ForecastTimerange(long from, long to) {
		this.from = from;
		this.to = to;
	}

	private long from;

	private long to;

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
		return new StringBuilder().append(new Date(from)).append(" - ").append(new Date(to)).toString();
	}

}
