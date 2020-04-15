package org.continuity.api.entities.config.cobra;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.continuity.api.entities.config.ServiceConfiguration;
import org.continuity.idpa.AppId;
import org.continuity.lctl.schema.ContextSchema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Configuration for the cobra service.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "service", "app-id", "time-zone", "traces", "tailoring", "sessions", "clustering", "intensity", "context" })
public class CobraConfiguration implements ServiceConfiguration {

	public static final String SERVICE = "cobra";

	private static final List<List<String>> DEFAULT_TAILORING = Collections.singletonList(Collections.singletonList(AppId.SERVICE_ALL));

	private AppId appId;

	private TracesConfiguration traces = new TracesConfiguration();

	private SessionsConfiguration sessions = new SessionsConfiguration();

	private List<List<String>> tailoring = DEFAULT_TAILORING;

	private ContextSchema context = new ContextSchema();

	private ClusteringConfiguration clustering = new ClusteringConfiguration();

	private IntensityConfiguration intensity = new IntensityConfiguration();

	@JsonProperty("time-zone")
	@JsonSerialize(converter = ZoneIdToStringConverter.class)
	@JsonDeserialize(converter = StringToZoneIdConverter.class)
	private ZoneId timeZone = ZoneId.systemDefault();

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
	 * Defines how traces are treated.
	 *
	 * @return
	 */
	public TracesConfiguration getTraces() {
		return traces;
	}

	public void setTraces(TracesConfiguration traces) {
		this.traces = traces;
	}

	/**
	 * Defines how sessions are generated.
	 *
	 * @return
	 */
	public SessionsConfiguration getSessions() {
		if (sessions == null) {
			synchronized (this) {
				if (sessions == null) {
					sessions = new SessionsConfiguration();
				}
			}
		}

		return sessions;
	}

	public void setSessions(SessionsConfiguration sessions) {
		this.sessions = sessions;
	}

	/**
	 * Defines the tailoring as multiple service combinations.
	 *
	 * @return A list of service combinations. For each combination, sessions will be created
	 *         automatically. Default is {@code [[ all ]]}.
	 */
	public List<List<String>> getTailoring() {
		return tailoring == null ? DEFAULT_TAILORING : tailoring;
	}

	public void setTailoring(List<List<String>> tailoring) {
		this.tailoring = tailoring;
	}

	/**
	 * Defines the context schema, i.e., the type of variables that are used.
	 *
	 * @return
	 */
	public ContextSchema getContext() {
		if (context == null) {
			synchronized (this) {
				if (context == null) {
					context = new ContextSchema();
				}
			}
		}

		return context;
	}

	public void setContext(ContextSchema context) {
		this.context = context;
	}

	/**
	 * Defines the clustering that is done automatically.
	 *
	 * @return
	 */
	public ClusteringConfiguration getClustering() {
		if (clustering == null) {
			synchronized (this) {
				if (clustering == null) {
					clustering = new ClusteringConfiguration();
				}
			}
		}

		return clustering;
	}

	public void setClustering(ClusteringConfiguration clustering) {
		this.clustering = clustering;
	}

	public IntensityConfiguration getIntensity() {
		if (intensity == null) {
			synchronized (this) {
				if (intensity == null) {
					intensity = new IntensityConfiguration();
				}
			}
		}

		return intensity;
	}

	public void setIntensity(IntensityConfiguration intensity) {
		this.intensity = intensity;
	}

	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(ZoneId timeZone) {
		this.timeZone = timeZone;
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

	public static class ZoneIdToStringConverter extends StdConverter<ZoneId, String> {

		@Override
		public String convert(ZoneId value) {
			value.toString();
			return Objects.toString(value);
		}

	}

	public static class StringToZoneIdConverter extends StdConverter<String, ZoneId> {

		@Override
		public ZoneId convert(String value) {
			return ZoneId.of(value);
		}

	}

}
