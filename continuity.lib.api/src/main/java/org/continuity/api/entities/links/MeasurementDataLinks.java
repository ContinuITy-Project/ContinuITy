package org.continuity.api.entities.links;

import java.lang.reflect.Field;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MeasurementDataLinks extends AbstractLinks<MeasurementDataLinks> {

	@JsonProperty(value = "link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String link;

	@JsonProperty("timestamp")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH-mm-ss-SSSX")
	@JsonInclude(Include.NON_NULL)
	private Date timestamp;
	
	@JsonProperty("type")
	private MeasurementDataLinkType linkType;

	public MeasurementDataLinks(LinkExchangeModel parent) {
		super(parent);
	}

	public MeasurementDataLinks() {
		this(null);
	}

	public String getLink() {
		return link;
	}

	public MeasurementDataLinks setLink(String externalDataLink) {
		this.link = externalDataLink;
		return this;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public MeasurementDataLinks setTimestamp(Date externalDataTimestamp) {
		this.timestamp = externalDataTimestamp;
		return this;
	}
	
	public MeasurementDataLinkType getLinkType() {
		return linkType;
	}

	public void setLinkType(MeasurementDataLinkType linkType) {
		this.linkType = linkType;
	}


	@Override
	public boolean isEmpty() {
		for (Field field : MeasurementDataLinks.class.getDeclaredFields()) {
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
	public void merge(MeasurementDataLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : MeasurementDataLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
