package org.continuity.cli.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.entities.TestPlanBundle;
import org.continuity.cli.process.JMeterProcess;
import org.continuity.commons.jmeter.JMeterPropertiesCorrector;
import org.continuity.commons.jmeter.TestPlanWriter;
import org.continuity.commons.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
@ShellComponent
public class JMeterCommands {

	private static final String KEY_JMETER_HOME = "jmeter.home";

	private static final String KEY_JMETER_CONFIG = "jmeter.configuration";

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private RestTemplate restTemplate;

	private TestPlanWriter testPlanWriter;

	private JMeterPropertiesCorrector propertiesCorrector = new JMeterPropertiesCorrector();

	@ShellMethod(key = { "jmeter-home" }, value = "Sets the home directory of JMeter (where the bin directory is placed).")
	public String setJMeterHome(String jmeterHome) {
		jmeterHome = jmeterHome.replace("\\", "/");
		Object old = propertiesProvider.get().put(KEY_JMETER_HOME, jmeterHome);
		return old == null ? "Set JMeter home." : "Replaced old JMeter home: " + old;
	}

	@ShellMethod(key = { "jmeter-config" }, value = "Sets the configuration directory of JMeter.")
	public String setJMeterConfig(String jmeterConfig) {
		jmeterConfig = jmeterConfig.replace("\\", "/");
		Object old = propertiesProvider.get().put(KEY_JMETER_CONFIG, jmeterConfig);
		testPlanWriter = new TestPlanWriter(jmeterConfig);
		return old == null ? "Set JMeter config dir." : "Replaced old JMeter config dir: " + old;
	}

	@ShellMethod(key = { "create-jmeter-test" }, value = "Creates a load test with a tag from a workload model specified by type and link.")
	public String createLoadTest(String tag, String workloadType, String workloadId) throws IOException {
		String jmeterHome = propertiesProvider.get().getProperty(KEY_JMETER_HOME);

		if (testPlanWriter == null) {
			String jmeterConfig = propertiesProvider.get().getProperty(KEY_JMETER_CONFIG);

			if (jmeterConfig == null) {
				return "Please set the jmeter config path first (call 'jmeter-config [path]')";
			} else {
				testPlanWriter = new TestPlanWriter(jmeterConfig);
			}
		} else if (jmeterHome == null) {
			return "Please set the jmeter home path first (call 'jmeter-home [path]')";
		}

		Map<String, String> message = new HashMap<>();
		message.put("tag", tag);
		message.put("workload-type", workloadType);
		message.put("workload-id", workloadId);

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
		ResponseEntity<TestPlanBundle> response = restTemplate.getForEntity(url + "/loadtest/jmeter/" + workloadType + "/" + workloadId + "/create?tag=" + tag, TestPlanBundle.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			return response.toString();
		}

		Path testPlanDir = Paths.get(propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR), "jmeter-wessbas-" + workloadId);
		testPlanDir.toFile().mkdirs();

		TestPlanBundle testPlanBundle = response.getBody();
		propertiesCorrector.correctPaths(testPlanBundle.getTestPlan(), testPlanDir.toAbsolutePath());
		propertiesCorrector.configureResultFile(testPlanBundle.getTestPlan(), testPlanDir.resolve("results.csv").toAbsolutePath());
		propertiesCorrector.prepareForHeadlessExecution(testPlanBundle.getTestPlan());
		Path testPlanPath = testPlanWriter.write(testPlanBundle.getTestPlan(), testPlanBundle.getBehaviors(), testPlanDir);
		new JMeterProcess(jmeterHome).run(testPlanPath);

		return "Stored and opened JMeter test plan at " + testPlanPath;
	}

}
