package org.continuity.wessbas.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.continuity.wessbas.entities.MonitoringData;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.behaviormodelextractor.BehaviorModelExtractor;


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

		// Callback gets workloadmodel.xmi output from dslModelGenerator
		onModelCreatedCallback.accept(WessbasDslInstance.DVDSTORE_PARSED.get());
	}

	/**
	 * This method converts a session log into a Wessbas DSL instance.
	 * 
	 * @param sessionLog
	 */
	private void convertSessionLogIntoWessbasDSLInstance(String sessionLog) {
		// Write the session log String into sessions.dat file
		// Trigger projects
		// Return workloadmodel.xmi file
		writeSessionLogIntoFile(sessionLog);
		BehaviorModelExtractor behav = new BehaviorModelExtractor();
		behav.createBehaviorModel();
	}

	private void writeSessionLogIntoFile(String sessionLog) {
		try {
			FileOutputStream fout = FileUtils.openOutputStream(new File("examples/inspectit/input/sessions.dat"));
			PrintStream ps = new PrintStream(fout);

			StringBuffer entry = new StringBuffer();
			entry.append(sessionLog);

			ps.print(entry.toString());

			ps.close();
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Sends request to Session Logs webservice and gets Session Log
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