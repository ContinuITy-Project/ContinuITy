package org.continuity.api.entities.exchange;

import java.lang.reflect.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Henning Schulz
 *
 */
public class BehaviorModelLinks extends AbstractLinks<BehaviorModelLinks> {

	@JsonProperty(value = "type", required = false)
	@JsonInclude(Include.NON_NULL)
	private BehaviorModelType type;

	@JsonProperty(value = "link", required = false)
	@JsonInclude(Include.NON_NULL)
	private String link;

	public BehaviorModelLinks(ArtifactExchangeModel parent) {
		super(parent);
	}

	public BehaviorModelLinks() {
		this(null);
	}

	public BehaviorModelType getType() {
		return type;
	}

	public BehaviorModelLinks setType(BehaviorModelType type) {
		this.type = type;
		return this;
	}

	public String getLink() {
		return link;
	}

	public BehaviorModelLinks setLink(String link) {
		this.link = link;
		return this;
	}

	@Override
	public boolean isEmpty() {
		for (Field field : BehaviorModelLinks.class.getDeclaredFields()) {
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
	public void merge(BehaviorModelLinks other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : BehaviorModelLinks.class.getDeclaredFields()) {
			if ((field.getName() != "parent") && (field.get(this) == null)) {
				field.set(this, field.get(other));
			}
		}
	}

}
