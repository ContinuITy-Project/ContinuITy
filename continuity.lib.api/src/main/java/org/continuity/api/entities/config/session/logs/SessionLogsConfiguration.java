package org.continuity.api.entities.config.session.logs;

import java.time.Duration;
import java.util.Objects;

import org.continuity.api.entities.config.ServiceConfiguration;
import org.continuity.idpa.AppId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Configuration for the session-logs service.
 *
 * @author Henning Schulz
 *
 */
public class SessionLogsConfiguration implements ServiceConfiguration {

	public static final String SERVICE = "session-logs";

	public static final Duration DEFAULT_MAX_SESSION_PAUSE = Duration.ofMinutes(30);

	private AppId appId;

	@JsonProperty("max-session-pause")
	@JsonSerialize(converter = DurationToStringConverter.class)
	@JsonDeserialize(converter = StringToDurationConverter.class)
	private Duration maxSessionPause = DEFAULT_MAX_SESSION_PAUSE;

	@Override
	public String getService() {
		return SERVICE;
	}

	@Override
	public AppId getAppId() {
		return appId;
	}

	public void setAppId(AppId appId) {
		this.appId = appId;
	}

	/**
	 *
	 * @return The maximum time of inactivity a session can have. After that, a new session starts.
	 */
	public Duration getMaxSessionPause() {
		return maxSessionPause;
	}

	public void setMaxSessionPause(Duration maxSessionPause) {
		this.maxSessionPause = maxSessionPause;
	}

	@Override
	public void init(AppId aid) {
		setAppId(aid);
	}

	public static class DurationToStringConverter extends StdConverter<Duration, String> {

		@Override
		public String convert(Duration value) {
			return Objects.toString(value);
		}

	}

	public static class StringToDurationConverter extends StdConverter<String, Duration> {

		@Override
		public Duration convert(String value) {
			return Duration.parse(value);
		}

	}

}
