package org.continuity.api.entities.order;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "approach", "total" })
public class ForecastOptions {

	@JsonInclude(Include.NON_NULL)
	private String approach;

	@JsonInclude(Include.NON_ABSENT)
	private Optional<Boolean> total = Optional.empty();

	public ForecastOptions(String approach, Boolean total) {
		this.approach = approach;
		this.total = Optional.ofNullable(total);
	}

	public ForecastOptions(String approach) {
		this(approach, null);
	}

	public ForecastOptions(boolean total) {
		this(null, total);
	}

	public ForecastOptions() {
	}

	public String getApproach() {
		return approach;
	}

	@JsonIgnore
	public String getApproachOrDefault() {
		return approach == null ? "Telescope" : approach;
	}

	public void setApproach(String approach) {
		this.approach = approach;
	}

	public Optional<Boolean> getTotal() {
		return total;
	}

	public void setTotal(Optional<Boolean> total) {
		this.total = total;
	}
}
