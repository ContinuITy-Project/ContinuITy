package org.continuity.session.logs.extractor;

import org.continuity.api.entities.artifact.markovbehavior.AbsoluteMarkovChain;
import org.continuity.api.entities.artifact.markovbehavior.AbsoluteMarkovTransition;
import org.continuity.api.entities.artifact.markovbehavior.AbstractMarkovChain;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionRequest;

/**
 * Can aggregate a list of sessions into a Markov chain.
 *
 * @author Henning Schulz
 *
 */
public class SessionsToMarkovChainAggregator {

	/**
	 * Aggregates the given sessions into a Markov chain.
	 *
	 * @param sessions
	 * @return
	 */
	public RelativeMarkovChain aggregate(Iterable<Session> sessions) {
		AbsoluteMarkovChain chain = new AbsoluteMarkovChain();

		for (Session session : sessions) {
			String last = AbstractMarkovChain.INITIAL_STATE;
			long lastExit = Long.MAX_VALUE;

			for (SessionRequest request : session.getRequests()) {
				String next = request.getEndpoint();

				updateTransition(chain, last, next, request.getStartMicros() - lastExit);

				last = next;
				lastExit = request.getEndMicros();
			}

			updateTransition(chain, last, AbstractMarkovChain.FINAL_STATE, 0);
		}

		return chain.toRelativeMarkovChain();
	}

	private void updateTransition(AbsoluteMarkovChain chain, String from, String to, long thinkTimeMicros) {
		AbsoluteMarkovTransition transition = chain.getTransition(from, to);
		transition.increment(Math.max(0, thinkTimeMicros / 1000));
		chain.setTransition(from, to, transition);
	}

}
