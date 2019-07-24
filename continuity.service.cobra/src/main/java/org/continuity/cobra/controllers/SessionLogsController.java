package org.continuity.cobra.controllers;

import static org.continuity.api.rest.RestApi.Cobra.Sessions.Paths.GET_EXTENDED;
import static org.continuity.api.rest.RestApi.Cobra.Sessions.Paths.GET_SIMPLE;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionView;
import org.continuity.api.rest.RestApi;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 *
 * @author Alper Hi
 * @author Henning Schulz
 *
 */
@RestController
@RequestMapping(RestApi.Cobra.Sessions.ROOT)
public class SessionLogsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsController.class);

	@Autowired
	private ElasticsearchSessionManager elasticManager;

	/**
	 * Provides session logs stored in the database in simple formatting.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The services to which the logs are to be tailored, separated by '.'.
	 * @param from
	 *            The start date of the sessions.
	 * @param to
	 *            The end date of the sessions.
	 * @return The session logs as string.
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET_SIMPLE, method = RequestMethod.GET, produces = { "text/plain" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getSimpleSessionLogs(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) String from,
			@RequestParam(required = false) String to) throws IOException, TimeoutException {
		return getSessionLogs(aid, tailoring, from, to, true);
	}

	/**
	 * Provides session logs stored in the database in extended formatting.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The services to which the logs are to be tailored, separated by '.'.
	 * @param from
	 *            The start date of the sessions.
	 * @param to
	 *            The end date of the sessions.
	 * @return The session logs as string.
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET_EXTENDED, method = RequestMethod.GET, produces = { "text/plain" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getExtendedSessionLogs(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) String from,
			@RequestParam(required = false) String to) throws IOException, TimeoutException {
		return getSessionLogs(aid, tailoring, from, to, false);
	}

	private ResponseEntity<String> getSessionLogs(AppId aid, String tailoring, String from, String to, boolean simple) throws IOException, TimeoutException {
		Triple<BadRequestResponse, Date, Date> check = checkDates(from, to);

		if (check.getLeft() != null) {
			return check.getLeft().toStringResponse();
		}

		List<Session> sessions = elasticManager.readSessionsInRange(aid, null, Session.convertStringToTailoring(tailoring), check.getMiddle(), check.getRight());

		if ((sessions == null) || sessions.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		String logs = sessions.stream().map(simple ? Session::toSimpleLog : Session::toExtensiveLog).collect(Collectors.joining("\n"));

		return ResponseEntity.ok(logs);
	}

	/**
	 * Provides simple sessions stored in the database.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The services to which the logs are to be tailored, separated by '.'.
	 * @param from
	 *            The start date of the sessions.
	 * @param to
	 *            The end date of the sessions.
	 * @return {@link Session}
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET_SIMPLE, method = RequestMethod.GET, produces = { "application/json" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	@JsonView(SessionView.Simple.class)
	public ResponseEntity<?> getSessionsAsSimpleJson(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) String from,
			@RequestParam(required = false) String to) throws IOException, TimeoutException {
		return getSessionsAsJson(aid, tailoring, from, to, true);
	}

	/**
	 * Provides extended sessions stored in the database.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The services to which the logs are to be tailored, separated by '.'.
	 * @param from
	 *            The start date of the sessions.
	 * @param to
	 *            The end date of the sessions.
	 * @return {@link Session}
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET_EXTENDED, method = RequestMethod.GET, produces = { "application/json" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	@JsonView(SessionView.Extended.class)
	public ResponseEntity<?> getSessionsAsExtendedJson(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) String from,
			@RequestParam(required = false) String to) throws IOException, TimeoutException {
		return getSessionsAsJson(aid, tailoring, from, to, false);
	}

	public ResponseEntity<?> getSessionsAsJson(AppId aid, String tailoring, String from, String to, boolean simple) throws IOException, TimeoutException {
		Triple<BadRequestResponse, Date, Date> check = checkDates(from, to);

		if (check.getLeft() != null) {
			return check.getLeft().toObjectResponse();
		}

		List<Session> sessions = elasticManager.readSessionsInRange(aid, null, Session.convertStringToTailoring(tailoring), check.getMiddle(), check.getRight());

		if ((sessions == null) || sessions.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(sessions);
	}

	private Triple<BadRequestResponse, Date, Date> checkDates(String from, String to) {
		Date dFrom = null;

		if (from != null) {
			try {
				dFrom = ApiFormats.DATE_FORMAT.parse(from);
			} catch (ParseException e) {
				LOGGER.error("Cannot parse from date: {}", e.getMessage());
				return Triple.of(new BadRequestResponse("Illegal date format of 'from' date: " + from), null, null);
			}
		}

		Date dTo = null;

		if (to != null) {
			try {
				dTo = ApiFormats.DATE_FORMAT.parse(to);
			} catch (ParseException e) {
				LOGGER.error("Cannot parse to date: {}", e.getMessage());
				return Triple.of(new BadRequestResponse("Illegal date format of 'to' date: " + to), null, null);
			}
		}

		return Triple.of(null, dFrom, dTo);
	}

	@JsonView(SessionView.Simple.class)
	private static class BadRequestResponse {

		private String message;

		public BadRequestResponse(String message) {
			this.message = message;
		}

		@SuppressWarnings("unused")
		public String getMessage() {
			return message;
		}

		private ResponseEntity<String> toStringResponse() {
			return ResponseEntity.badRequest().body(message);
		}

		private ResponseEntity<BadRequestResponse> toObjectResponse() {
			return ResponseEntity.badRequest().body(this);
		}

	}

}
