package org.continuity.cobra.controllers;

import static org.continuity.api.rest.RestApi.Cobra.Sessions.Paths.GET_EXTENDED;
import static org.continuity.api.rest.RestApi.Cobra.Sessions.Paths.GET_SIMPLE;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
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
	 * @param overlapping
	 *            If {@code true}, all sessions overlapping the range will be returned; if
	 *            {@code false}, only those starting within the range will be returned.
	 * @return The session logs as string.
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET_SIMPLE, method = RequestMethod.GET, produces = { "text/plain" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getSimpleSessionLogs(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) List<String> from,
			@RequestParam(required = false) List<String> to, @RequestParam(name = "overlapping", defaultValue = "true") boolean overlapping) throws IOException, TimeoutException {
		return getSessionLogs(aid, tailoring, from, to, true, overlapping);
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
	 * @param overlapping
	 *            If {@code true}, all sessions overlapping the range will be returned; if
	 *            {@code false}, only those starting within the range will be returned.
	 * @return The session logs as string.
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET_EXTENDED, method = RequestMethod.GET, produces = { "text/plain" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<String> getExtendedSessionLogs(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) List<String> from,
			@RequestParam(required = false) List<String> to, @RequestParam(name = "overlapping", defaultValue = "true") boolean overlapping) throws IOException, TimeoutException {
		return getSessionLogs(aid, tailoring, from, to, false, overlapping);
	}

	private ResponseEntity<String> getSessionLogs(AppId aid, String tailoring, List<String> from, List<String> to, boolean simple, boolean overlap) throws IOException, TimeoutException {
		Pair<BadRequestResponse, List<Session>> sessions = readSessions(aid, tailoring, from, to, overlap);

		if (sessions.getLeft() != null) {
			return sessions.getLeft().toStringResponse();
		} else if ((sessions.getRight() == null) || sessions.getRight().isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		String logs = sessions.getRight().stream().map(simple ? Session::toSimpleLog : Session::toExtensiveLog).collect(Collectors.joining("\n"));

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
	 * @param overlapping
	 *            If {@code true}, all sessions overlapping the range will be returned; if
	 *            {@code false}, only those starting within the range will be returned.
	 * @return {@link Session}
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET_SIMPLE, method = RequestMethod.GET, produces = { "application/json" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	@JsonView(SessionView.Simple.class)
	public ResponseEntity<?> getSessionsAsSimpleJson(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) List<String> from,
			@RequestParam(required = false) List<String> to, @RequestParam(name = "overlapping", defaultValue = "true") boolean overlapping) throws IOException, TimeoutException {
		return getSessionsAsJson(aid, tailoring, from, to, true, overlapping);
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
	 * @param overlapping
	 *            If {@code true}, all sessions overlapping the range will be returned; if
	 *            {@code false}, only those starting within the range will be returned.
	 * @return {@link Session}
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(value = GET_EXTENDED, method = RequestMethod.GET, produces = { "application/json" })
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path") })
	@JsonView(SessionView.Extended.class)
	public ResponseEntity<?> getSessionsAsExtendedJson(@ApiIgnore @PathVariable("app-id") AppId aid, @PathVariable String tailoring, @RequestParam(required = false) List<String> from,
			@RequestParam(required = false) List<String> to, @RequestParam(name = "overlapping", defaultValue = "true") boolean overlapping) throws IOException, TimeoutException {
		return getSessionsAsJson(aid, tailoring, from, to, false, overlapping);
	}

	public ResponseEntity<?> getSessionsAsJson(AppId aid, String tailoring, List<String> from, List<String> to, boolean simple, boolean overlap) throws IOException, TimeoutException {
		Pair<BadRequestResponse, List<Session>> sessions = readSessions(aid, tailoring, from, to, overlap);

		if (sessions.getLeft() != null) {
			return sessions.getLeft().toObjectResponse();
		} else if ((sessions.getRight() == null) || sessions.getRight().isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(sessions.getRight());
	}

	private Pair<BadRequestResponse, List<Session>> readSessions(AppId aid, String tailoring, List<String> from, List<String> to, boolean overlap) throws IOException, TimeoutException {
		if ((from == null) && (to == null)) {
			return Pair.of(null, elasticManager.readSessionsOverlapping(aid, null, Session.convertStringToTailoring(tailoring), null, null));
		} else {
			Iterator<Pair<String, String>> rangeIter = IntStream.range(0, Math.max(sizeOf(from), sizeOf(to))).mapToObj(i -> Pair.of(elementAt(from, i), elementAt(to, i))).iterator();

			Set<Session> sessions = new HashSet<>();

			while (rangeIter.hasNext()) {
				Pair<String, String> range = rangeIter.next();

				Pair<BadRequestResponse, Date> checkF = checkDate("from", range.getLeft());

				if (checkF.getLeft() != null) {
					return Pair.of(checkF.getLeft(), null);
				}

				Pair<BadRequestResponse, Date> checkT = checkDate("to", range.getRight());

				if (checkT.getLeft() != null) {
					return Pair.of(checkT.getLeft(), null);
				}

				if (overlap) {
					sessions.addAll(elasticManager.readSessionsOverlapping(aid, null, Session.convertStringToTailoring(tailoring), checkF.getRight(), checkT.getRight()));
				} else {
					sessions.addAll(elasticManager.readSessionsStartingInRange(aid, null, Session.convertStringToTailoring(tailoring), checkF.getRight(), checkT.getRight()));
				}
			}

			return Pair.of(null, new ArrayList<>(sessions));
		}
	}

	private int sizeOf(List<?> list) {
		return list == null ? 0 : list.size();
	}

	private <T> T elementAt(List<T> list, int i) {
		if ((list == null) || (list.size() <= i)) {
			return null;
		} else {
			return list.get(i);
		}
	}

	private Pair<BadRequestResponse, Date> checkDate(String name, String date) {
		Date dDate = null;

		if (NumberUtils.isNumber(date)) {
			dDate = new Date(NumberUtils.createNumber(date).longValue());
		} else if (date != null) {
			try {
				dDate = ApiFormats.DATE_FORMAT.parse(date);
			} catch (ParseException e) {
				LOGGER.error("Cannot parse from date: {}", e.getMessage());
				return Pair.of(new BadRequestResponse("Illegal date format of '" + name + "' date: " + date), null);
			}
		}

		return Pair.of(null, dDate);
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
