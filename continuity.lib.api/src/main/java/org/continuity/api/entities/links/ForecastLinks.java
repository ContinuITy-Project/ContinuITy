package org.continuity.api.entities.links;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ForecastLinks extends AbstractLinks<ForecastLinks> {

	@JsonProperty(value = "link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String link;

	public ForecastLinks(LinkExchangeModel parent) {
		super(parent);
	}

	public ForecastLinks() {
		this(null);
	}

	public String getLink() {
		return link;
	}

	public ForecastLinks setLink(String forecastLink) {
		this.link = forecastLink;
		return this;
	}

	@Override
	public boolean isEmpty() {
		for (Field field : ForecastLinks.class.getDeclaredFields()) {
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
	public void merge(ForecastLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : ForecastLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
