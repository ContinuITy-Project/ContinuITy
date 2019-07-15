package org.continuity.session.logs.extractor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.idpa.VersionOrTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and updates sessions with new requests.
 *
 * @author Henning Schulz
 *
 */
public class SessionUpdater {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionUpdater.class);

	private final VersionOrTimestamp version;

	private final List<String> services;

	private final long maxSessionPauseMicros;

	private final boolean forceFinish;

	public SessionUpdater(VersionOrTimestamp version, List<String> services, long maxSessionPauseMicros, boolean forceFinish) {
		this.version = version;
		this.services = services;
		this.maxSessionPauseMicros = maxSessionPauseMicros;
		this.forceFinish = forceFinish;
	}

	/**
	 * Updates the sessions with the requests.
	 *
	 * @param oldSessions
	 *            The already existing sessions.
	 * @param requests
	 *            The requests to be used for updating.
	 * @return A list of updated sessions. Will only contain sessions that have been changed.
	 */
	public Set<Session> updateSessions(List<Session> oldSessions, List<SessionRequest> requests) {
		int numOld = oldSessions.size();
		int numNew = 0;
		AtomicInteger numFinished = new AtomicInteger(0);
		int numDuplicate = 0;
		int numRedirect = 0;
		int numError = 0;

		Collections.sort(requests);
		Map<String, Session> sessionPerId = oldSessions.stream().collect(Collectors.toMap(Session::getId, s -> s));

		Set<Session> newSessions = new HashSet<>();
		long maxDate = 0;

		for (SessionRequest req : requests) {
			Session session = sessionPerId.get(req.getSessionId());

			if (session == null) {
				session = createFreshSession(req.getSessionId());
				sessionPerId.put(req.getSessionId(), session);
				numNew++;
			}

			if ((session.getRequests().size() > 0) && ((req.getStartMicros() - session.getEndMicros()) > maxSessionPauseMicros)) {
				session.setFinished(true);
				newSessions.add(session);
				numFinished.incrementAndGet();

				session = createFreshSession(req.getSessionId());
				sessionPerId.put(req.getSessionId(), session);
				numNew++;
			}

			if (!session.getRequests().contains(req)) {
				if (!session.isFresh() && (req.getStartMicros() < session.getStartMicros())) {
					LOGGER.error("Cannot add request {} to non-fresh session {}! Start date is before session start date! Ignoring the request.", req, session);
					numError++;
					continue;
				}

				if ((session.getRequests().size() > 0) && isRedirect(session.getRequests().floor(req))) {
					numRedirect++;
					continue;
				}

				session.addRequest(req);

				newSessions.add(session);
				maxDate = Math.max(maxDate, req.getEndMicros());
			} else {
				numDuplicate++;
			}
		}

		int numUpdated = newSessions.size() - numNew;

		Stream<Session> toBeFinished;

		if (forceFinish) {
			toBeFinished = sessionPerId.values().stream();
		} else {
			final long sessionHorizon = maxDate - maxSessionPauseMicros;
			toBeFinished = sessionPerId.values().stream().filter(s -> (s.getEndMicros() < sessionHorizon));
		}

		toBeFinished.forEach(s -> {
			s.setFinished(true);
			newSessions.add(s);
			numFinished.incrementAndGet();
		});

		LOGGER.info("Session clustering done. old: {}, updated: {}, new: {}, finished: {}, ignored (redirect): {}, ignored (duplicate): {}, ignored (error): {}", numOld, numUpdated, numNew,
				numFinished, numRedirect, numDuplicate, numError);

		return newSessions;
	}

	private boolean isRedirect(SessionRequest request) {
		return (request.getExtendedInformation() != null) && ((request.getExtendedInformation().getResponseCode() / 100) == 3);
	}

	private Session createFreshSession(String sessionId) {
		Session session = new Session();

		session.setFresh(true);
		session.setId(sessionId);
		session.setVersion(version);
		session.setTailoring(services);

		return session;
	}

}
