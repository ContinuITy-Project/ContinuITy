package org.continuity.api.entities.config.cobra;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.continuity.api.entities.config.ServiceConfiguration;
import org.continuity.dsl.schema.ContextSchema;
import org.continuity.idpa.AppId;

import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * Configuration for the cobra service.
 *
 * @author Henning Schulz
 *
 */
public class CobraConfiguration implements ServiceConfiguration {

	public static final String SERVICE = "cobra";

	private static final List<List<String>> DEFAULT_TAILORING = Collections.singletonList(Collections.singletonList(AppId.SERVICE_ALL));

	private AppId appId;

	private SessionsConfiguration sessions = new SessionsConfiguration();

	private List<List<String>> tailoring = DEFAULT_TAILORING;

	private ContextSchema context = new ContextSchema();

	private ClusteringConfiguration clustering = new ClusteringConfiguration();

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
