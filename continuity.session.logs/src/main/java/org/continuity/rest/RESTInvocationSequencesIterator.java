package org.continuity.rest;

import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;

/**
 * Iterator for iterating across the pagign mechanism of the inpsectIT rest API.
 * @author Jonas Kunz
 *
 */
public class RESTInvocationSequencesIterator implements Iterator<InvocationSequenceData> {

	private static final String INVOCATION_PATH = "/rest/data/invocations";

	private static final String OVERVIEW_PARAM_LAST_ID = "latestReadId";
	private static final String DETAILS_PARAM_ID = "id";

	private JsonHTTPClientWrapper rest;

	private Map<String,?> filterParams;

	private boolean endReached;
	private long lastSequenceID;

	private Deque<Long> invocationSequenceIdQueue;

	public RESTInvocationSequencesIterator(JsonHTTPClientWrapper rest, Map<String,?> filterParams) {
		this.rest = rest;
		this.filterParams = filterParams;

		endReached = false;
		lastSequenceID = -1;
		invocationSequenceIdQueue = new LinkedList<>();

	}

	private void fetchNextIds() {


		Map<String,String> params = new HashMap<String, String>();
		filterParams.forEach((a,b) -> params.put(a, b.toString()));
		params.put(OVERVIEW_PARAM_LAST_ID,""+( lastSequenceID+1));

		InvocationSequenceData[] result;
		try {
			result = rest.performGet(INVOCATION_PATH, InvocationSequenceData[].class, params);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(result.length == 0) {
			//no more sequences returned -> end reached
			endReached = true;
		} else {
			//add the ids to the fetch queue
			Stream.of(result)
			.map(InvocationSequenceData::getId)
			.forEach(invocationSequenceIdQueue::add);
			//update the last fetched index

			lastSequenceID =
					Stream.of(result)
					.mapToLong(InvocationSequenceData::getId)
					.max().getAsLong();
		}

	}

	@Override
	public boolean hasNext() {
		if(endReached) {
			return false;
		} else {
			if(invocationSequenceIdQueue.isEmpty()) {
				fetchNextIds();
				return !endReached;
			} else {
				return true;
			}
		}
	}

	@Override
	public InvocationSequenceData next() {
		//this call also guaranties that the nextIds queue is not empty
		if(!hasNext()) {
			throw new IllegalStateException("no Furhter sequences available");
		}

		long idToFetch = invocationSequenceIdQueue.pollFirst();

		Map<String,String> params = new HashMap<String, String>();
		filterParams.forEach((a,b) -> params.put(a, b.toString()));
		params.put(DETAILS_PARAM_ID, ""+idToFetch);

		try {
			String INVOCATION_DETAILS_PATH = INVOCATION_PATH + "/" + idToFetch;
			return rest.performGet(INVOCATION_DETAILS_PATH, InvocationSequenceData.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


	}

}
