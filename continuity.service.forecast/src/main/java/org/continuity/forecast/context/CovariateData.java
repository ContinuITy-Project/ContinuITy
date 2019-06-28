package org.continuity.forecast.context;

import java.util.List;

import org.continuity.idpa.AppId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author Alper Hidiroglu
 */
@JsonPropertyOrder({ "app-id", "covar-name", "values" })
public class CovariateData {

	@JsonProperty("app-id")
	private AppId appId;

	@JsonProperty("covar-name")
	private String covarName;

	private List<CovariateValue> values;

	public AppId getAppId() {
		return appId;
	}

	public void setAppId(AppId appId) {
		this.appId = appId;
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
		return "CovariateData [app-id=" + appId + ", covarName=" + covarName + ", values=" + values + "]";
	}

}
