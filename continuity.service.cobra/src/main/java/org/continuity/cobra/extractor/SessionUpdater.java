package org.continuity.cobra.extractor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

	private static final Set<Integer> REDIRECT_CODES = Arrays.asList(300, 301, 302, 303, 305, 307, 308).stream().collect(Collectors.toSet());

	private final VersionOrTimestamp version;

	private final long maxSessionPauseMicros;

	private final boolean forceFinish;

	private final boolean ignoreRedirects;

	public SessionUpdater(VersionOrTimestamp version, long maxSessionPauseMicros, boolean forceFinish, boolean ignoreRedirects) {
		this.version = version;
		this.maxSessionPauseMicros = maxSessionPauseMicros;
		this.forceFinish = forceFinish;
		this.ignoreRedirects = ignoreRedirects;
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
	public Set<Session> updateSessions(List<Session> oldSessions, List<? extends SessionRequest> requests) {
		int numOld = oldSessions.size();
		int numNew = 0;
		AtomicInteger numFinished = new AtomicInteger(0);
		int numDuplicate = 0;
		int numError = 0;

		Collections.sort(requests);
		Map<String, Session> sessionPerId = oldSessions.stream().collect(Collectors.toMap(Session::getSessionId, s -> s));

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

		int numRedirect = 0;

		if (!ignoreRedirects) {
			numRedirect = newSessions.stream().mapToInt(this::removeRedirects).sum();
		}

		LOGGER.info("Session grouping done. old: {}, updated: {}, new: {}, finished: {}, ignored (redirect): {}, ignored (duplicate): {}, ignored (error): {}", numOld, numUpdated, numNew, numFinished,
				numRedirect, numDuplicate, numError);

		return newSessions;
	}

	private int removeRedirects(Session session) {
		if (session.getRequests().isEmpty()) {
			return 0;
		}

		Iterator<SessionRequest> iter = session.getRequests().descendingIterator();
		Set<SessionRequest> toRemove = new TreeSet<>();

		SessionRequest curr = iter.next();
		SessionRequest last = curr;

		while (iter.hasNext()) {
			SessionRequest prev = iter.next();

			if (isRedirect(prev)) {
				toRemove.add(curr);
			}

			curr = prev;
		}

		if (session.isRedirectEnding()) {
			toRemove.add(curr);
		}

		session.setRedirectEnding(isRedirect(last));
		session.getRequests().removeAll(toRemove);

		return toRemove.size();
	}

	private boolean isRedirect(SessionRequest request) {
		return !ignoreRedirects && (request != null) && (request.getExtendedInformation() != null) && REDIRECT_CODES.contains(request.getExtendedInformation().getResponseCode());
	}

	private Session createFreshSession(String sessionId) {
		Session session = new Session();

		session.setFresh(true);
		session.setSessionId(sessionId);
		session.setVersion(version);

		return session;
	}

}
