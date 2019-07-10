package org.continuity.session.logs.extractor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.commons.openxtrace.OpenXtraceTracer;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Location;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import open.xtrace.OPENxtraceUtils;

/**
 * Tailors requests to a given set of services.
 *
 * @author Henning Schulz
 *
 */
public class RequestTailorer {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestTailorer.class);

	private final AppId aid;

	private final VersionOrTimestamp version;

	private final RestTemplate restTemplate;

	/**
	 *
	 * @param aid
	 *            The app-id (the service part will be ignored).
	 * @param version
	 *            The version of the services.
	 * @param restTemplate
	 *            {@link RestTemplate} to be used for retrieving the application models.
	 */
	public RequestTailorer(AppId aid, VersionOrTimestamp version, RestTemplate restTemplate) {
		this.aid = aid;
		this.version = version;
		this.restTemplate = restTemplate;
	}

	/**
	 * Tailors a list of traces to a given list of services.
	 *
	 * @param services
	 *            The list of target services (will be appended to the app-id).
	 * @param traces
	 *            The traces to be tailored.
	 * @return A list of {@link SessionRequest}s per tailored requests.
	 */
	public List<SessionRequest> tailorTraces(List<String> services, List<Trace> traces) {
		ResponseEntity<Application[]> response;
		try {
			response = restTemplate.getForEntity(
					RestApi.Orchestrator.Idpa.GET_APPLICATION.requestUrl(aid).withQuery("version", version.toString()).withQuery("services", services.stream().collect(Collectors.joining(","))).get(),
					Application[].class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Could not get application models!", e);
			return Collections.emptyList();
		}

		LOGGER.info("{}@{}: Retrieved application models for services {}.", aid.getApplication(), version, services);

		List<Application> applications = Arrays.asList(response.getBody());

		if (applications.contains(null)) {
			LOGGER.error("{}@{} Application models contained null, meaning that at least one application is missing!", aid.getApplication(), version);
			return Collections.emptyList();
		}

		// TODO: assuming the endpoints are HttpEndpoints.
		List<String> hostNames = applications.stream().map(Application::getEndpoints).flatMap(List::stream).map(HttpEndpoint.class::cast).map(HttpEndpoint::getDomain).filter(Objects::nonNull)
				.distinct().collect(Collectors.toList());

		LOGGER.info("{}@{} Extracted the following host names: {}", aid.getApplication(), version, hostNames);

		MultiRequestMapper mapper = new MultiRequestMapper(applications);

		List<SessionRequest> requests = traces.stream().map(t -> extractChildRequests(t, hostNames)).flatMap(List::stream).map(mapper::mapToEndpoint).filter(Objects::nonNull).map(this::mapToSession)
				.collect(Collectors.toList());

		LOGGER.info("{}@{} Tailoring done.", aid.getApplication(), version);

		return requests;
	}

	private List<HTTPRequestProcessingImpl> extractChildRequests(Trace trace, List<String> targetHostNames) {
		return OpenXtraceTracer.forRootAndHosts(trace.getRoot().getRoot(), targetHostNames).extractSubtraces();
	}

	private SessionRequest mapToSession(Pair<HTTPRequestProcessingImpl, String> cae) {
		HTTPRequestProcessingImpl callable = cae.getLeft();
		String endpoint = cae.getRight();

		SessionRequest request = new SessionRequest();

		request.setSessionId(OPENxtraceUtils.extractSessionIdFromCookies(callable));
		request.setEndpoint(endpoint);
		request.setStartMicros(callable.getTimestamp() * 1000);
		request.setEndMicros((callable.getTimestamp() * 1000) + (callable.getResponseTime() / 1000));

		if (callable.getIdentifier().isPresent()) {
			request.setId(callable.getIdentifier().get().toString());
		} else {
			request.setId(Integer.toHexString(request.hashCode()));
		}

		return request;
	}

	private class MultiRequestMapper {

		private final List<RequestUriMapper> mappers;

		private MultiRequestMapper(List<Application> applications) {
			mappers = applications.stream().map(RequestUriMapper::new).collect(Collectors.toList());
		}

		private Pair<HTTPRequestProcessingImpl, String> mapToEndpoint(HTTPRequestProcessingImpl callable) {
			for (RequestUriMapper uriMapper : mappers) {
				HttpEndpoint endpoint = uriMapper.map(callable.getUri(), callable.getRequestMethod().get().name());

				Location location = callable.getContainingSubTrace().getLocation();

				if ((endpoint != null) && endpoint.getDomain().equals(location.getHost()) && endpoint.getPort().equals(Integer.toString(location.getPort()))) {
					return Pair.of(callable, endpoint.getId());
				}
			}

			return null;
		}

	}

}
