package org.continuity.forecast.context;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author Alper Hidiroglu
 */
@JsonPropertyOrder({ "tag", "covar-name", "values" })
public class CovariateData {
	
	private String tag;

	@JsonProperty("covar-name")
	private String covarName;
	
	private List<CovariateValue> values;
	
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getCovarName() {
		return covarName;
	}

	public void setCovarName(String covarName) {
		this.covarName = covarName;
	}

	public List<CovariateValue> getValues() {
		return values;
	}

	public void setValues(List<CovariateValue> values) {
		this.values = values;
	}
	
	@Override
	public String toString() {
		return "CovariateData [tag=" + tag + ", covarName=" + covarName + ", values=" + values + "]";
	}

}
