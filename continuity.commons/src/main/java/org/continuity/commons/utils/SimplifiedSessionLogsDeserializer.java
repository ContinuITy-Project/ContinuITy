package org.continuity.commons.utils;

import java.util.ArrayList;
import java.util.List;

import org.continuity.api.entities.artifact.SimplifiedSession;

/**
 * Util class for parsing
 * @author Tobias Angerstein
 *
 */
public class SimplifiedSessionLogsDeserializer {

	/**
	 * Parse session logs to simplied sessions
	 * 
	 * @param sessionLogsString
	 * @return a list containing all {@link SimplifiedSession} elements
	 */
	public static List<SimplifiedSession> parse(String sessionLogsString) {
		List<SimplifiedSession> sessionLogs = new ArrayList<SimplifiedSession>();
		String[] sessionLogsArray = sessionLogsString.split("\n");
		for (String sessionLogString : sessionLogsArray) {
			String[] sessionLogRequests = sessionLogString.split(";");
			String sessionId = sessionLogRequests[0];
			String firstRequest = sessionLogRequests[1];
			String lastRequest = sessionLogRequests[sessionLogRequests.length - 1];

			long startTimeStamp = Long.parseLong(firstRequest.split(":")[1]);
			long endTimeStamp = Long.parseLong(lastRequest.split(":")[2]);

			sessionLogs.add(new SimplifiedSession(sessionId, startTimeStamp, endTimeStamp));
		}
		return sessionLogs;
	}
}
