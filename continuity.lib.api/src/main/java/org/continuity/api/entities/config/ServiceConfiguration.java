package org.continuity.api.entities.config;

import org.continuity.api.entities.config.session.logs.SessionLogsConfiguration;
import org.continuity.idpa.AppId;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Common interface for service configurations.
 *
 * @author Henning Schulz
 *
 */
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "service")
@JsonSubTypes({ @Type(value = SessionLogsConfiguration.class, name = SessionLogsConfiguration.SERVICE) })
@JsonPropertyOrder({ "service", "app-id" })
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ServiceConfiguration {

	/**
	 * Gets the ContinuITy service to be configured.
	 *
	 * @return
	 */
	String getService();

	/**
	 * Gets the app-id to be configured.
	 *
	 * @return
	 */
	@JsonProperty("app-id")
	AppId getAppId();

	/**
	 * Initializes the configuration with an app-id.
	 *
	 * @param aid
	 */
	void init(AppId aid);

}
