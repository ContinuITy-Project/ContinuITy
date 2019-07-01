package org.continuity.api.entities.links;

import java.lang.reflect.Field;
import java.util.Date;

import org.continuity.api.entities.ApiFormats;

import com.fasterxml.jackson.annotation.JsonFormat;

public class TraceLinks extends AbstractLinks<TraceLinks> {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiFormats.DATE_FORMAT_PATTERN)
	private Date from;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiFormats.DATE_FORMAT_PATTERN)
	private Date to;

	public TraceLinks(LinkExchangeModel parent) {
		super(parent);
	}

	public TraceLinks() {
		this(null);
	}

	public Date getFrom() {
		return from;
	}

	public TraceLinks setFrom(Date from) {
		this.from = from;
		return this;
	}

	public Date getTo() {
		return to;
	}

	public TraceLinks setTo(Date to) {
		this.to = to;
		return this;
	}

	@Override
	public boolean isEmpty() {
		for (Field field : TraceLinks.class.getDeclaredFields()) {
			try {
				if ((field.getName() != "parent") && (field.get(this) != null)) {
					return false;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	@Override
	public void merge(TraceLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : TraceLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
