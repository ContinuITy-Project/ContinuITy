package org.continuity.wessbas.managers;

import java.util.function.Consumer;

import org.continuity.wessbas.entities.MonitoringData;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;

/**
 * Manages the WESSBAS pipeline from the input data to the output WESSBAS DSL
 * instance.
 *
 * @author Henning Schulz, Alper Hi
 *
 */
public class WessbasPipelineManager {

	private final Consumer<WorkloadModel> onModelCreatedCallback;

	/**
	 * Constructor.
	 *
	 * @param onModelCreatedCallback
	 *            The function to be called when the model was created.
	 */
	public WessbasPipelineManager(Consumer<WorkloadModel> onModelCreatedCallback) {
		this.onModelCreatedCallback = onModelCreatedCallback;
	}

	/**
	 * Runs the pipeline and calls the callback when the model was created.
	 *
	 * TODO: Implement
	 *
	 * @param data
	 *            Input monitoring data to be transformed into a WESSBAS DSL
	 *            instance.
	 */
	public void runPipeline(MonitoringData data) {

		String sessionLog = getSessionLog(data);

		convertSessionLogIntoWessbasDSLInstance(sessionLog);

		onModelCreatedCallback.accept(WessbasDslInstance.DVDSTORE_PARSED.get());
	}

	/**
	 * This method converts a session log into a Wessbas DSL instance.
	 * 
	 * @param sessionLog
	 */
	private void convertSessionLogIntoWessbasDSLInstance(String sessionLog) {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public String getSessionLog(MonitoringData data) {
			
		RestTemplate restTemplate = new RestTemplate();
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		String urlString = "http://session-logs";
		map.add("link", data.getLink());
		String sessionLog = restTemplate.postForObject(urlString, map, String.class);
		System.out.println(sessionLog.toString());
		
		return sessionLog;
	}
}