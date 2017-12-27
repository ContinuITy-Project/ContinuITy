package org.continuity.rest;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import rocks.inspectit.shared.all.cmr.model.MethodIdent;
import rocks.inspectit.shared.all.cmr.model.PlatformIdent;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.cmr.ApplicationData;
import rocks.inspectit.shared.all.communication.data.cmr.BusinessTransactionData;


/**
 *
 * Class for managing the REST connection with an inpsectIT CMR.
 *
 * @author Jonas Kunz, Alper Hidiroglu
 *
 */
public class InspectITRestClient {

	private static final String PARAM_PLATFOMR_ID = "platformId";


	private static final String ALL_AGENTS_PATH = "/rest/data/agents";

	private static final String ALL_METHODS_PATH = "/rest/data/agents";

	private static final String ALL_APPLICATIONS_PATH = "/rest/bc/app";

	private static final String CREATE_STORAGE_PATH = "/rest/storage/create";
	private static final String START_RECORDING_STORAGE_PATH = "/rest/storage/start-recording";
	private static final String STOP_RECORDING_STORAGE_PATH = "/rest/storage/stop-recording";
	private static final String DELETE_STORAGE_PATH = "/rest/storage/delete";

	private JsonHTTPClientWrapper rest;


	/**
	 * @param hostWithPort host:port of the CMR to connect to
	 */
	public InspectITRestClient(String hostWithPort) {
		super();
		rest = new JsonHTTPClientWrapper(hostWithPort);
	}

	/**
	 * Fetches all Invocation Sequences from the CMRs buffer for the given agent.
	 *
	 * @param platformID the id of the agent
	 * @return
	 */
	public Iterable<InvocationSequenceData> fetchAll(long platformID){
		final Map<String,Object> filterParams = new HashMap<>();
		filterParams.put(PARAM_PLATFOMR_ID, platformID);

		return new Iterable<InvocationSequenceData>() {

			@Override
			public Iterator<InvocationSequenceData> iterator() {
				return new RESTInvocationSequencesIterator(rest,filterParams);
			}
		};
	}

	/**
	 * @return A list of all Agents known to the CMR
	 * @throws IOException
	 */
	public Iterable<ApplicationData> fetchAllApplications() throws IOException {

		ApplicationData[] result = rest.performGet(ALL_APPLICATIONS_PATH, ApplicationData[].class);

		return Arrays.asList(result);
	}

	/**
	 * @return A list of all Agents known to the CMR
	 * @throws IOException
	 */
	public Iterable<BusinessTransactionData> fetchAllBusinessTransactions(int appId) throws IOException {

		BusinessTransactionData[] result = rest.performGet(ALL_APPLICATIONS_PATH + "/" + appId + "/btx", BusinessTransactionData[].class);

		return Arrays.asList(result);
	}

	/**
	 * @return A list of all Agents known to the CMR
	 * @throws IOException
	 */
	public Iterable<PlatformIdent> fetchAllAgents() throws IOException {

		PlatformIdent[] result = rest.performGet(ALL_AGENTS_PATH, PlatformIdent[].class);

		return Arrays.asList(result);
	}


	/**
	 * Fetches all Method Identifiers of instrumented Methods for a given Agent.
	 *
	 * @param platformId the agent to fetch from
	 * @return all MethodIdents
	 * @throws IOException
	 */
	public Iterable<MethodIdent> fetchAllMethods(long platformId) throws IOException {

		Map<String,String> params = new HashMap<>();
		params.put(PARAM_PLATFOMR_ID, ""+platformId);
		MethodIdent[] result = rest.performGet(ALL_METHODS_PATH + "/" + platformId + "/methods", MethodIdent[].class, params);

		return Arrays.asList(result);
	}

	/**
	 * Creates a new storage at the CMR.
	 *
	 * @param name the name of the storage.
	 * @return the id of the storage.
	 * @throws IOException
	 */
	public String createStorage(String name) throws IOException {
		Map<String,String> params = new HashMap<>();
		params.put("name", name);
		Map result = rest.performGet(CREATE_STORAGE_PATH, Map.class, params);
		return (String) ((Map)result.get("storage")).get("id");
	}

	/**
	 * Starts the recording of a given storage.
	 * @param id the id of the storage
	 * @throws IOException
	 */
	public void startRecording(String id) throws IOException {
		Map<String,String> params = new HashMap<>();
		params.put("id", id);
		params.put("autoFinalize", "true");
		Map result = rest.performGet(START_RECORDING_STORAGE_PATH, Map.class, params);
	}


	/**
	 * Deletes a storage.
	 * @param id the id of the storage to delete
	 * @throws IOException
	 */
	public void deleteStorage(String id) throws IOException {
		Map<String,String> params = new HashMap<>();
		params.put("id", id);
		Map result = rest.performGet(DELETE_STORAGE_PATH, Map.class, params);
	}

	/**
	 * Stops the recording.
	 * @throws IOException
	 */
	public void stopRecording() throws IOException {
		Map<String,String> params = new HashMap<>();
		Map result = rest.performGet(STOP_RECORDING_STORAGE_PATH, Map.class, params);
	}

	/**
	 * Closes the connection.
	 */
	public void close() {
		rest.destroy();
	}

}
