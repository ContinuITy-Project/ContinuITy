package org.continuity.wessbas.managers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.continuity.commons.wessbas.WessbasModelParser;
import org.continuity.wessbas.entities.MonitoringData;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.behaviormodelextractor.BehaviorModelExtractor;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;
import net.sf.markov4jmeter.m4jdslmodelgenerator.M4jdslModelGenerator;

/**
 * Manages the WESSBAS pipeline from the input data to the output WESSBAS DSL
 * instance.
 *
 * @author Henning Schulz, Alper Hi
 *
 */
public class WessbasPipelineManager {

	private final Consumer<WorkloadModel> onModelCreatedCallback;
	
	private RestTemplate restTemplate;

	/**
	 * Constructor.
	 *
	 * @param onModelCreatedCallback
	 *            The function to be called when the model was created.
	 */
	public WessbasPipelineManager(Consumer<WorkloadModel> onModelCreatedCallback, RestTemplate restTemplate) {
		this.onModelCreatedCallback = onModelCreatedCallback;
		this.restTemplate = restTemplate;
	}
	
	/**
	 * Runs the pipeline and calls the callback when the model was created.
	 *
	 *
	 * @param data
	 *            Input monitoring data to be transformed into a WESSBAS DSL
	 *            instance.
	 */
	public void runPipeline(MonitoringData data) {

		String sessionLog = getSessionLog(data);

		convertSessionLogIntoWessbasDSLInstance(sessionLog);
		
		WessbasModelParser parser = new WessbasModelParser();
		InputStream inputStream = null;
		try {
			inputStream = FileUtils.openInputStream(new File("wessbas/modelgenerator/workloadmodel.xmi"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Callback gets workloadmodel.xmi output from dslModelGenerator
		try {
			onModelCreatedCallback.accept(parser.readWorkloadModel(inputStream));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		createWorkloadIntensity();
		BehaviorModelExtractor behav = new BehaviorModelExtractor();
		behav.createBehaviorModel();
		M4jdslModelGenerator generator = new M4jdslModelGenerator();
		try {
			generator.generate();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneratorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void createWorkloadIntensity() {
		String workloadDefinition = "workloadIntensity.type=constant\r\n" + "wl.type.value=800";
		try {
			FileOutputStream fout = FileUtils.openOutputStream(new File("wessbas/workloadIntensity.properties"));
			PrintStream ps = new PrintStream(fout);

			StringBuffer entry = new StringBuffer();
			entry.append(workloadDefinition);

			ps.print(entry.toString());

			ps.close();
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeSessionLogIntoFile(String sessionLog) {
		try {
			FileOutputStream fout = FileUtils.openOutputStream(new File("wessbas/sessions.dat"));
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

		//MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		String urlString = "http://session-logs?link=" + data.getLink();
		//map.add("link", data.getLink());
		String sessionLog = this.restTemplate.getForObject(urlString, String.class);
		System.out.println(sessionLog.toString());

		return sessionLog;
	}
}