package org.continuity.idpa.controllers;

import static org.continuity.api.rest.RestApi.Idpa.Application.ROOT;
import static org.continuity.api.rest.RestApi.Idpa.Application.Paths.GET;
import static org.continuity.api.rest.RestApi.Idpa.Application.Paths.GET_AS_REGEX;
import static org.continuity.api.rest.RestApi.Idpa.Application.Paths.GET_DELTA;
import static org.continuity.api.rest.RestApi.Idpa.Application.Paths.UPDATE;
import static org.continuity.api.rest.RestApi.Idpa.Application.Paths.UPDATE_FROM_WORKLOAD_MODEL;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.entities.ApplicationModelLink;
import org.continuity.idpa.entities.EndpointAsRegex;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.continuity.idpa.storage.ApplicationStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import io.swagger.annotations.ApiParam;

/**
 * Offers a REST API for controlling the stored system models.
 *
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(ROOT)
public class ApplicationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationController.class);

	private static final DateFormat DATE_FORMAT = ApiFormats.DATE_FORMAT;

	private final RestTemplate restTemplate;

	private final AmqpTemplate amqpTemplate;

	@Value("${spring.application.name}")
	private String applicationName;

	private final ApplicationStorageManager manager;

	@Autowired
	public ApplicationController(ApplicationStorageManager manager, RestTemplate restTemplate, AmqpTemplate amqpTemplate) {
		this.manager = manager;
		this.restTemplate = restTemplate;
		this.amqpTemplate = amqpTemplate;
	}

	/**
	 * Retrieves the current application model. If the timestamp is {@code null}, the latest version
	 * will be returned.
	 *
	 * @param tag
	 *            The tag of the application.
	 * @param timestamp
	 *            The timestamp for which the application model is searched (optional).
	 * @return The current application model.
	 */
	@RequestMapping(path = GET, method = RequestMethod.GET)
	public ResponseEntity<Application> getApplication(@PathVariable String tag, @RequestParam(required = false) String timestamp) {
		if (timestamp == null) {
			try {
				return ResponseEntity.ok(manager.read(tag));
			} catch (IOException e) {
				LOGGER.error("An exception occured during reading!", e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
		} else {
			Date date;
			try {
				date = DATE_FORMAT.parse(timestamp);
			} catch (ParseException e) {
				LOGGER.error("Could not parse timestamp {}.", timestamp);
				LOGGER.error("Exception:", e);
				return ResponseEntity.badRequest().build();
			}

			try {
				return ResponseEntity.ok(manager.read(tag, date));
			} catch (IOException e) {
				LOGGER.error("An exception occured during reading!", e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
		}
	}

	/**
	 * Gets a report holding the difference between the current application model and a date in the
	 * past.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param since
	 *            The date to compare against in the format {@link ApiFormats#DATE_FORMAT}.
	 * @return The delta report.
	 */
	@RequestMapping(path = GET_DELTA, method = RequestMethod.GET)
	public ResponseEntity<ApplicationChangeReport> getDeltaSince(@PathVariable String tag, @RequestParam("since") String since) {
		Date date;
		try {
			date = DATE_FORMAT.parse(since);
		} catch (ParseException e) {
			LOGGER.error("Could not parse since date {}.", since);
			LOGGER.error("Exception:", e);
			return ResponseEntity.badRequest().build();
		}

		return ResponseEntity.ok(manager.getDeltaSince(tag, date));
	}

	/**
	 * Retrieves the application model stored for the specified tag and transforms it to a map of
	 * regular expressions. Currently only supports {@link HttpEndpoint}, i.e., all other types
	 * won't be contained.
	 *
	 * @param tag
	 *            The tag of the application.
	 * @return A map of endpoint IDs to request methods and regular expressions to be applied to
	 *         check whether a certain request matches the endpoint, e.g.,
	 *         {@code ^/?my/(?<id>[^/]*)/path/(?<rest>.*)/?$} for the abstract path
	 *         <code>@code /my/{id}/path/{rest:*}</code>. Url parameters can be extracted via the
	 *         corresponding named capture groups.
	 */
	@RequestMapping(path = GET_AS_REGEX, method = RequestMethod.GET)
	public ResponseEntity<Map<String, EndpointAsRegex>> getApplicationAsRegex(@PathVariable String tag) {
		Application app;
		try {
			app = manager.read(tag);
		} catch (IOException e) {
			LOGGER.error("An exception occured during reading!", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		Map<String, EndpointAsRegex> regexPerEndpoint = app.getEndpoints().stream().filter(HttpEndpoint.class::isInstance).map(HttpEndpoint.class::cast)
				.collect(Collectors.toMap(HttpEndpoint::getId, this::toRegex));

		return ResponseEntity.ok(regexPerEndpoint);
	}

	private EndpointAsRegex toRegex(HttpEndpoint endpoint) {
		String path = endpoint.getPath();
		String regex = null;

		if (path != null) {
			if (path.startsWith("/")) {
				path = path.substring(1);
			}

			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}

			regex = new StringBuilder().append("^/?").append(path.replaceAll("\\{([^\\}]*)\\:\\*\\}", "(?<$1>.*)").replaceAll("\\{([^\\}]*)\\}", "(?<$1>[^/]*)")).append("/?$").toString();
		}

		return new EndpointAsRegex(endpoint.getDomain(), endpoint.getPort(), endpoint.getMethod(), regex);
	}

	/**
	 * Stores a new application model if it differs from existing ones, possibly ignoring specific
	 * change types.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param system
	 *            The system model to be stored.
	 * @param ignoreInterfaceChanged
	 *            Ignore {@link ApplicationChangeType#ENDPOINT_CHANGED}.
	 * @param ignoreInterfaceRemoved
	 *            Ignore {@link ApplicationChangeType#ENDPOINT_REMOVED}.
	 * @param ignoreInterfaceAdded
	 *            Ignore {@link ApplicationChangeType#ENDPOINT_ADDED}.
	 * @param ignoreParameterRemoved
	 *            Ignore {@link ApplicationChangeType#PARAMETER_REMOVED}.
	 * @param ignoreParameterAdded
	 *            Ignore {@link ApplicationChangeType#PARAMETER_ADDED}.
	 * @return A report holding the differences between the passed model and the next older one.
	 */
	@RequestMapping(path = UPDATE, method = RequestMethod.POST)
	public ResponseEntity<String> updateApplication(@PathVariable String tag, @RequestBody Application system,
			@RequestParam(name = "ignore-interface-changed", defaultValue = "false") boolean ignoreInterfaceChanged,
			@RequestParam(name = "ignore-interface-removed", defaultValue = "false") boolean ignoreInterfaceRemoved,
			@RequestParam(name = "ignore-interface-added", defaultValue = "false") boolean ignoreInterfaceAdded,
			@RequestParam(name = "ignore-parameter-changed", defaultValue = "false") boolean ignoreParameterChanged,
			@RequestParam(name = "ignore-parameter-removed", defaultValue = "false") boolean ignoreParameterRemoved,
			@RequestParam(name = "ignore-parameter-added", defaultValue = "false") boolean ignoreParameterAdded) {

		EnumSet<ApplicationChangeType> ignoredChangeTypes = changeTypesFromBooleans(ignoreInterfaceChanged, ignoreInterfaceRemoved, ignoreInterfaceAdded, ignoreParameterChanged, ignoreParameterRemoved,
				ignoreParameterAdded);

		ApplicationChangeReport report = manager.saveOrUpdate(tag, system, ignoredChangeTypes);

		if (report.changed()) {
			try {
				amqpTemplate.convertAndSend(AmqpApi.Idpa.EVENT_CHANGED.name(), AmqpApi.Idpa.EVENT_CHANGED.formatRoutingKey().of(tag),
						new ApplicationModelLink(applicationName, tag, report.getBeforeChange()));
			} catch (AmqpException e) {
				LOGGER.error("Could not send the system model with tag {} to the {} exchange!", tag, AmqpApi.Idpa.EVENT_CHANGED.name());
				LOGGER.error("Exception:", e);
			}
		}

		return ResponseEntity.ok().body(report.toString());
	}

	/**
	 * Stores a new or updates an application model if it differs from existing ones, possibly
	 * ignoring specific change types. The application model is retrieved from a workload model.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param workloadModelLink
	 *            The link to the workload model.
	 * @param ignoreInterfaceChanged
	 *            Ignore {@link ApplicationChangeType#ENDPOINT_CHANGED}.
	 * @param ignoreInterfaceRemoved
	 *            Ignore {@link ApplicationChangeType#ENDPOINT_REMOVED}.
	 * @param ignoreInterfaceAdded
	 *            Ignore {@link ApplicationChangeType#ENDPOINT_ADDED}.
	 * @param ignoreParameterRemoved
	 *            Ignore {@link ApplicationChangeType#PARAMETER_REMOVED}.
	 * @param ignoreParameterAdded
	 *            Ignore {@link ApplicationChangeType#PARAMETER_ADDED}.
	 * @return A report holding the differences between the passed model and the next older one.
	 */
	@RequestMapping(path = UPDATE_FROM_WORKLOAD_MODEL, method = RequestMethod.POST)
	public void updateApplicationFromWorkloadModel(@PathVariable String tag, @RequestBody String workloadModelLink,
			@RequestParam(name = "ignore-interface-changed", defaultValue = "false") boolean ignoreInterfaceChanged,
			@RequestParam(name = "ignore-interface-removed", defaultValue = "false") boolean ignoreInterfaceRemoved,
			@RequestParam(name = "ignore-interface-added", defaultValue = "false") boolean ignoreInterfaceAdded,
			@RequestParam(name = "ignore-parameter-changed", defaultValue = "false") boolean ignoreParameterChanged,
			@RequestParam(name = "ignore-parameter-removed", defaultValue = "false") boolean ignoreParameterRemoved,
			@RequestParam(name = "ignore-parameter-added", defaultValue = "false") boolean ignoreParameterAdded) {
		LOGGER.info("Updating IDPA from workload model link: {}", workloadModelLink);

		ResponseEntity<LinkExchangeModel> workloadLinksResponse;
		try {
			workloadLinksResponse = restTemplate.getForEntity(WebUtils.addProtocolIfMissing(workloadModelLink), LinkExchangeModel.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Could not retrieve the workload model overview from {}. Got response code {}!", workloadModelLink, e.getStatusCode());
			LOGGER.error("Exception:", e);
			return;
		}

		LinkExchangeModel link = workloadLinksResponse.getBody();

		if ("INVALID".equals(link.getWorkloadModelLinks().getApplicationLink())) {
			LOGGER.error("Received invalid system model link: {}", link);
			return;
		}

		ResponseEntity<Application> systemResponse;
		try {
			systemResponse = restTemplate.getForEntity(WebUtils.addProtocolIfMissing(link.getWorkloadModelLinks().getApplicationLink()), Application.class);
		} catch (HttpStatusCodeException e) {
			LOGGER.error("Could not retrieve the system model from {}. Got response code {}!", link.getWorkloadModelLinks().getApplicationLink(), e.getStatusCode());
			LOGGER.error("Exception:", e);
			return;
		}

		Application systemModel = systemResponse.getBody();

		updateApplication(tag, systemModel, ignoreInterfaceChanged, ignoreInterfaceRemoved, ignoreInterfaceAdded, ignoreParameterChanged, ignoreParameterRemoved, ignoreParameterAdded);
	}

	/**
	 * Stores a new application model if it differs from existing ones, possibly ignoring specific
	 * change types.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param system
	 *            The application model in YAML format.
	 * @param ignoreInterfaceChanged
	 *            Ignore {@link ApplicationChangeType#ENDPOINT_CHANGED}.
	 * @param ignoreInterfaceRemoved
	 *            Ignore {@link ApplicationChangeType#ENDPOINT_REMOVED}.
	 * @param ignoreInterfaceAdded
	 *            Ignore {@link ApplicationChangeType#ENDPOINT_ADDED}.
	 * @param ignoreParameterRemoved
	 *            Ignore {@link ApplicationChangeType#PARAMETER_REMOVED}.
	 * @param ignoreParameterAdded
	 *            Ignore {@link ApplicationChangeType#PARAMETER_ADDED}.
	 * @return A report holding the differences between the passed model and the next older one.
	 */
	@RequestMapping(path = UPDATE, method = RequestMethod.PUT)
	public ResponseEntity<String> updateApplicationYaml(@PathVariable String tag,
			@ApiParam(value = "The application model in YAML format.", required = true) @RequestBody String application,
			@RequestParam(name = "ignore-interface-changed", defaultValue = "false") boolean ignoreInterfaceChanged,
			@RequestParam(name = "ignore-interface-removed", defaultValue = "false") boolean ignoreInterfaceRemoved,
			@RequestParam(name = "ignore-interface-added", defaultValue = "false") boolean ignoreInterfaceAdded,
			@RequestParam(name = "ignore-parameter-changed", defaultValue = "false") boolean ignoreParameterChanged,
			@RequestParam(name = "ignore-parameter-removed", defaultValue = "false") boolean ignoreParameterRemoved,
			@RequestParam(name = "ignore-parameter-added", defaultValue = "false") boolean ignoreParameterAdded) {

		EnumSet<ApplicationChangeType> ignoredChangeTypes = changeTypesFromBooleans(ignoreInterfaceChanged, ignoreInterfaceRemoved, ignoreInterfaceAdded, ignoreParameterChanged, ignoreParameterRemoved,
				ignoreParameterAdded);

		IdpaYamlSerializer<Application> serializer = new IdpaYamlSerializer<>(Application.class);
		Application system;
		try {
			system = serializer.readFromYamlString(application);
		} catch (IOException e) {
			LOGGER.error("Exception during reading application model with tag {}!", tag);
			LOGGER.error("Exception: " , e);
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		ApplicationChangeReport report = manager.saveOrUpdate(tag, system, ignoredChangeTypes);

		if (report.changed()) {
			try {
				amqpTemplate.convertAndSend(AmqpApi.Idpa.EVENT_CHANGED.name(), AmqpApi.Idpa.EVENT_CHANGED.formatRoutingKey().of(tag),
						new ApplicationModelLink(applicationName, tag, report.getBeforeChange()));
			} catch (AmqpException e) {
				LOGGER.error("Could not send the system model with tag {} to the {} exchange!", tag, AmqpApi.Idpa.EVENT_CHANGED.name());
				LOGGER.error("Exception: ", e);
			}
		}

		return ResponseEntity.ok().body(report.toString());
	}

	/**
	 * Stores a new application model if it differs from existing ones. None of the change types
	 * will be ignored.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param system
	 *            The application model to be stored.
	 * @return A report holding the differences between the passed model and the next older one.
	 */
	public ResponseEntity<String> updateApplication(@PathVariable String tag, @RequestBody Application system) {
		return updateApplication(tag, system, false, false, false, false, false, false);
	}

	private EnumSet<ApplicationChangeType> changeTypesFromBooleans(boolean ignoreInterfaceChanged, boolean ignoreInterfaceRemoved, boolean ignoreInterfaceAdded, boolean ignoreParameterChanged,
			boolean ignoreParameterRemoved,
			boolean ignoreParameterAdded) {
		EnumSet<ApplicationChangeType> set = EnumSet.noneOf(ApplicationChangeType.class);

		if (ignoreInterfaceChanged) {
			set.add(ApplicationChangeType.ENDPOINT_CHANGED);
		}

		if (ignoreInterfaceRemoved) {
			set.add(ApplicationChangeType.ENDPOINT_REMOVED);
		}

		if (ignoreInterfaceAdded) {
			set.add(ApplicationChangeType.ENDPOINT_ADDED);
		}

		if (ignoreParameterChanged) {
			set.add(ApplicationChangeType.PARAMETER_CHANGED);
		}

		if (ignoreParameterRemoved) {
			set.add(ApplicationChangeType.PARAMETER_REMOVED);
		}

		if (ignoreParameterAdded) {
			set.add(ApplicationChangeType.PARAMETER_ADDED);
		}

		return set;
	}

}
