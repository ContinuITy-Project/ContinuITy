package org.continuity.cli.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.continuity.api.rest.RestApi.Frontend.Loadtest;
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
import org.springframework.shell.standard.ShellOption;
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

	@ShellMethod(key = { "create-jmeter-test" }, value = "Creates a load test with a tag from a workload model specified by a link.")
	public String createLoadTest(String tag, String workloadLink, @ShellOption(defaultValue = "1") int users, @ShellOption(defaultValue = "60") int duration,
			@ShellOption(defaultValue = "1") int rampup) throws IOException {
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

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
		String[] linkElements = workloadLink.split("\\/");
		String workloadType = linkElements[0];
		String workloadId = linkElements[linkElements.length - 1];
		ResponseEntity<TestPlanBundle> response = restTemplate.getForEntity(Loadtest.CREATE_AND_GET.requestUrl("jmeter", workloadType, workloadId).withQuery("tag", tag).withHost(url).get(),
				TestPlanBundle.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			return response.toString();
		}

		Path testPlanDir = Paths.get(propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR), extractTempDirPrefix(workloadLink));
		testPlanDir.toFile().mkdirs();

		TestPlanBundle testPlanBundle = response.getBody();
		propertiesCorrector.correctPaths(testPlanBundle.getTestPlan(), testPlanDir.toAbsolutePath());
		propertiesCorrector.configureResultFile(testPlanBundle.getTestPlan(), testPlanDir.resolve("results.csv").toAbsolutePath());
		propertiesCorrector.prepareForHeadlessExecution(testPlanBundle.getTestPlan());
		propertiesCorrector.setRuntimeProperties(testPlanBundle.getTestPlan(), users, duration, rampup);
		Path testPlanPath = testPlanWriter.write(testPlanBundle.getTestPlan(), testPlanBundle.getBehaviors(), testPlanDir);
		new JMeterProcess(jmeterHome).run(testPlanPath);

		return "Stored and opened JMeter test plan at " + testPlanPath;
	}

	private String extractTempDirPrefix(String workloadLink) {
		String[] tokens = workloadLink.split("/");
		return "jmeter-" + tokens[0] + "-" + tokens[2];
	}

}
