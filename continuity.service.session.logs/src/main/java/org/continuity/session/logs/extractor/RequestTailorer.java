package org.continuity.session.logs.extractor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.continuity.api.entities.artifact.session.ExtendedRequestInformation;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.commons.idpa.UrlPartParameterExtractor;
import org.continuity.commons.openxtrace.OpenXtraceTracer;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.session.logs.entities.TraceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Location;
import org.spec.research.open.xtrace.api.core.callables.HTTPRequestProcessing;
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

	private final boolean addPrePostProcessing;

	/**
	 *
	 * @param aid
	 *            The app-id (the service part will be ignored).
	 * @param version
	 *            The version of the services.
	 * @param restTemplate
	 *            {@link RestTemplate} to be used for retrieving the application models.
	 * @param addPrePostProcessing
	 *            Whether explicit entries for the pre and post processing should be added to the
	 *            sessions.
	 */
	public RequestTailorer(AppId aid, VersionOrTimestamp version, RestTemplate restTemplate, boolean addPrePostProcessing) {
		this.aid = aid;
		this.version = version;
		this.restTemplate = restTemplate;
		this.addPrePostProcessing = addPrePostProcessing;
	}

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
		this(aid, version, restTemplate, false);
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
	public List<SessionRequest> tailorTraces(List<String> services, List<TraceRecord> traces) {
		ResponseEntity<Application[]> response;
		try {
			response = restTemplate.getForEntity(
					RestApi.Idpa.Application.GET.requestUrl(aid).withQuery("version", version.toString()).withQuery("services", services.stream().collect(Collectors.joining(","))).get(),
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

	private List<RequestBundle> extractChildRequests(TraceRecord trace, List<String> targetHostNames) {
		OpenXtraceTracer tracer;

		if (targetHostNames.isEmpty()) {
			tracer = OpenXtraceTracer.forRoot(trace.getTrace().getRoot().getRoot());
		} else {
			tracer = OpenXtraceTracer.forRootAndHosts(trace.getTrace().getRoot().getRoot(), targetHostNames);
		}

		List<RequestBundle> childCallables = tracer.extractSubtraces().stream().map(this::normalBundle).collect(Collectors.toList());

		if (addPrePostProcessing) {
			List<HTTPRequestProcessingImpl> rootCallables = OpenXtraceTracer.forRoot(trace.getTrace().getRoot().getRoot()).extractSubtraces();

			if (rootCallables.size() > 0) {
				HTTPRequestProcessingImpl firstRoot = rootCallables.get(0);
				HTTPRequestProcessingImpl lastRoot = rootCallables.get(rootCallables.size() - 1);

				childCallables.add(0, preBundle(firstRoot, trace.getRawEndpoint()));
				childCallables.add(postBundle(lastRoot, trace.getRawEndpoint()));
			}
		}

		childCallables.forEach(b -> b.setTraceId(trace.getTrace().getTraceId()));

		return childCallables;
	}

	private SessionRequest mapToSession(RequestBundle bundle) {
		HTTPRequestProcessingImpl callable = bundle.getCallable();
		HttpEndpoint endpoint = bundle.getEndpoint();

		SessionRequest request = new SessionRequest();

		request.setSessionId(OPENxtraceUtils.extractSessionIdFromCookies(callable));
		request.setEndpoint(bundle.getEndpointName());
		request.setTraceId(bundle.getTraceId());
		request.setStartMicros(bundle.getStartMicros());
		request.setEndMicros(bundle.getEndMicros());

		if (callable.getIdentifier().isPresent()) {
			request.setId(callable.getIdentifier().get().toString());
		} else {
			request.setId(Integer.toHexString(request.hashCode()));
		}

		addExtendedInformation(request, callable, endpoint);

		return request;
	}

	private void addExtendedInformation(SessionRequest request, HTTPRequestProcessing callable, HttpEndpoint endpoint) {
		ExtendedRequestInformation info = new ExtendedRequestInformation();
		request.setExtendedInformation(info);

		info.setUri(callable.getUri());
		info.setParameters(toParameters(callable.getHTTPParameters(), callable.getRequestBody(), extractUriParams(callable.getUri(), endpoint.getPath())));
		info.setPort(callable.getContainingSubTrace().getLocation().getPort());
		info.setHost(callable.getContainingSubTrace().getLocation().getHost());

		if (callable.getRequestMethod().isPresent()) {
			info.setMethod(callable.getRequestMethod().get().name());
		}

		if (callable.getResponseCode().isPresent()) {
			info.setResponseCode(callable.getResponseCode().get().intValue());
		}
	}

	private String toParameters(Optional<Map<String, String[]>> httpParameters, Optional<String> body, Map<String, String[]> uriParams) {
		Map<String, String[]> params = new HashMap<String, String[]>();

		if (httpParameters.isPresent()) {
			params = Optional.ofNullable(httpParameters.get()).map(HashMap<String, String[]>::new).orElse(new HashMap<>());
		}

		// TODO: This is a workaround because the session logs do not support parameters without
		// values (e.g., host/login?logout) and WESSBAS fails if it is transformed to
		// host/login?logout=
		params = params.entrySet().stream().filter(e -> (e.getValue() != null) && (e.getValue().length > 0) && !"".equals(e.getValue()[0])).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		params.putAll(uriParams);

		if (body.isPresent() && !body.get().isEmpty()) {
			params.put("BODY", new String[] { body.get() });
		}

		if (params.isEmpty()) {
			return null;
		} else {
			return encodeQueryString(params);
		}
	}

	/**
	 * Extracts the parameters from the URI. E.g., if the URI pattern is
	 * <code>/foo/{bar}/get/{id}</code> and the actual URI is <code>/foo/abc/get/42</code>, the
	 * extracted parameters will be <code>URL_PART_bar=abc</code> and <code>URL_PARTid=42</code>.
	 *
	 * @param uri
	 *            The URI to extract the parameters from.
	 * @param urlPattern
	 *            The abstract URI that specifies the pattern.
	 * @return The extracted parameters in the form <code>[URL_PART_name -> value]</code>.
	 */
	private Map<String, String[]> extractUriParams(String uri, String urlPattern) {
		if (uri == null) {
			return Collections.emptyMap();
		}

		UrlPartParameterExtractor extractor = new UrlPartParameterExtractor(urlPattern, uri);
		Map<String, String[]> params = new HashMap<>();

		while (extractor.hasNext()) {
			String param = extractor.nextParameter();
			String value = extractor.currentValue();

			if (value == null) {
				throw new IllegalArgumentException("Uri and pattern need to have the same length, bus was '" + uri + "' and '" + urlPattern + "'!");
			}

			params.put("URL_PART_" + param, new String[] { value });
		}

		return params;
	}

	/**
	 * Encodes a map of parameters into a query string
	 *
	 * @param params
	 * @return
	 */
	protected String encodeQueryString(Map<String, String[]> params) {
		try {
			if (params.isEmpty()) {
				return null;
			}
			StringBuffer result = new StringBuffer();
			for (String key : params.keySet()) {
				String encodedKey = URLEncoder.encode(key, "UTF-8");
				for (String value : params.get(key)) {
					String encodedValue = "";
					if (value != null) {
						encodedValue = "=" + URLEncoder.encode(value, "UTF-8");
					}

					if (result.length() > 0) {
						result.append("&");
					}
					result.append(encodedKey + encodedValue);

				}
			}
			return result.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private class MultiRequestMapper {

		private final List<RequestUriMapper> mappers;

		private MultiRequestMapper(List<Application> applications) {
			mappers = applications.stream().map(RequestUriMapper::new).collect(Collectors.toList());
		}

		/**
		 * Maps the (non-pre/post) requests to endpoints.
		 *
		 * @param bundle
		 * @return A bundle containing the endpoint or {@code null} if there is no appropriate
		 *         endpoint.
		 */
		private RequestBundle mapToEndpoint(RequestBundle bundle) {
			if (bundle.isPrePost()) {
				return bundle;
			} else {
				for (RequestUriMapper uriMapper : mappers) {
					HttpEndpoint endpoint = uriMapper.map(bundle.getCallable().getUri(), bundle.getCallable().getRequestMethod().get().name());

					Location location = bundle.getCallable().getContainingSubTrace().getLocation();

					if ((endpoint != null) && areEqualOrNull(endpoint.getDomain(), location.getHost()) && areEqualOrNull(endpoint.getPort(), Integer.toString(location.getPort()))) {
						bundle.setEndpoint(endpoint);
						return bundle;
					}
				}
			}

			return null;
		}

	}

	private HttpEndpoint defaultEndpoint(Optional<String> bt) {
		HttpEndpoint endpoint = new HttpEndpoint();

		endpoint.setId(bt.orElse(""));
		endpoint.setDomain("");
		endpoint.setPort("80");
		endpoint.setMethod("GET");
		endpoint.setPath("/");

		return endpoint;
	}

	private boolean areEqualOrNull(Object expected, Object tested) {
		return (expected == null) || expected.equals(tested);
	}

	private RequestBundle normalBundle(HTTPRequestProcessingImpl callable) {
		return new RequestBundle(callable, null, false, false);
	}

	private RequestBundle preBundle(HTTPRequestProcessingImpl callable, HttpEndpoint rootEndpoint) {
		if (rootEndpoint == null) {
			rootEndpoint = defaultEndpoint(OPENxtraceUtils.getBusinessTransaction(callable));
		}

		return new RequestBundle(callable, rootEndpoint, true, false);
	}

	private RequestBundle postBundle(HTTPRequestProcessingImpl callable, HttpEndpoint rootEndpoint) {
		if (rootEndpoint == null) {
			rootEndpoint = defaultEndpoint(OPENxtraceUtils.getBusinessTransaction(callable));
		}

		return new RequestBundle(callable, rootEndpoint, false, true);
	}

	private class RequestBundle {

		private long traceId;

		private final HTTPRequestProcessingImpl callable;

		private HttpEndpoint endpoint;

		private final boolean pre;

		private final boolean post;

		public RequestBundle(HTTPRequestProcessingImpl callable, HttpEndpoint endpoint, boolean pre, boolean post) {
			this.callable = callable;
			this.endpoint = endpoint;
			this.pre = pre;
			this.post = post;
		}

		public long getTraceId() {
			return traceId;
		}

		public void setTraceId(long traceId) {
			this.traceId = traceId;
		}

		public HttpEndpoint getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(HttpEndpoint endpoint) {
			this.endpoint = endpoint;
		}

		public String getEndpointName() {
			if (isPrePost()) {
				return new StringBuilder().append(pre ? SessionRequest.PREFIX_PRE_PROCESSING : SessionRequest.PREFIX_POST_PROCESSING).append(endpoint.getId()).toString();
			} else {
				return endpoint.getId();
			}
		}

		public HTTPRequestProcessingImpl getCallable() {
			return callable;
		}

		public boolean isPrePost() {
			return pre || post;
		}

		public long getStartMicros() {
			if (post) {
				return callable.getExitTime() * 1000;
			} else {
				return callable.getTimestamp() * 1000;
			}
		}

		public long getEndMicros() {
			if (pre) {
				return callable.getTimestamp() * 1000;
			} else {
				return callable.getExitTime() * 1000;
			}
		}

	}

}
