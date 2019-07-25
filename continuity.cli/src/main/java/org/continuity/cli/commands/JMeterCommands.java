package org.continuity.cli.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jmeter.save.SaveService;
import org.apache.jorphan.collections.ListedHashTree;
import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.order.LoadTestType;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestEndpoint;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.process.JMeterProcess;
import org.continuity.cli.storage.OrderStorage;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.commons.jmeter.TestPlanWriter;
import org.continuity.idpa.AppId;
import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.markov4jmeter.testplangenerator.util.CSVHandler;

/**
 * @author Henning Schulz
 *
 */
@ShellComponent
@ShellCommandGroup("JMeter Commands")
public class JMeterCommands {

	private static final String CONTEXT_NAME = "jmeter";

	private static final String KEY_JMETER_HOME = "jmeter.home";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("home", this, "setJMeterHome", String.class), //
			new Shorthand("download", this, "downloadLoadTest", String.class), //
			new Shorthand("upload", this, "uploadLoadTest", String.class, String.class, boolean.class) //
	);

	private final CSVHandler csvHandler = new CSVHandler(CSVHandler.LINEBREAK_TYPE_UNIX);

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private OrderStorage storage;

	@Autowired
	private CliContextManager contextManager;

	@Autowired
	private ObjectMapper mapper;

	private TestPlanWriter testPlanWriter;

	@ShellMethod(key = { CONTEXT_NAME }, value = "Goes to the 'jmeter' context so that the shorthands can be used.")
	public AttributedString goToIdpaContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "jmeter home" }, value = "Sets the home directory of JMeter (where the bin directory is placed).")
	public String setJMeterHome(String jmeterHome) {
		jmeterHome = jmeterHome.replace("\\", "/");
		Object old = propertiesProvider.putProperty(KEY_JMETER_HOME, jmeterHome);
		testPlanWriter = new TestPlanWriter(jmeterHome);
		return old == null ? "Set JMeter home." : "Replaced old JMeter home: " + old;
	}

	@ShellMethod(key = { "jmeter download" }, value = "Downloads and opens a JMeter load test specified by a link.")
	public String downloadLoadTest(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String loadTestLink) throws IOException {
		String jmeterHome = propertiesProvider.getProperty(KEY_JMETER_HOME);

		String error = initTestPlanWriter(jmeterHome);

		if (!error.isEmpty()) {
			return error;
		}

		if (Shorthand.DEFAULT_VALUE.equals(loadTestLink)) {
			OrderReport report = storage.getReport(OrderStorage.ID_LATEST);

			if ((report == null) || (report.getArtifacts() == null) || (report.getArtifacts().getLoadTestLinks().getLink() == null)) {
				return "Cannot download the JMeter test of the latest order. The link is missing!";
			} else if (report.getArtifacts().getLoadTestLinks().getType() != LoadTestType.JMETER) {
				return "Cannot download the JMeter test of the latest order. The link points to a " + report.getArtifacts().getLoadTestLinks().getType().toPrettyString() + " test!";
			} else {
				loadTestLink = report.getArtifacts().getLoadTestLinks().getLink();
			}
		}

		ResponseEntity<JMeterTestPlanBundle> response = restTemplate.getForEntity(RestEndpoint.urlViaOrchestrator(loadTestLink, propertiesProvider.getProperty(PropertiesProvider.KEY_URL)),
				JMeterTestPlanBundle.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			return response.toString();
		}

		List<String> params = RestApi.JMeter.TestPlan.GET.parsePathParameters(loadTestLink);
		Path testPlanDir = Paths.get(propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR), params.get(0));
		testPlanDir.toFile().mkdirs();

		JMeterTestPlanBundle testPlanBundle = response.getBody();
		Path testPlanPath = testPlanWriter.write(testPlanBundle.getTestPlan(), testPlanBundle.getBehaviors(), testPlanDir);
		new JMeterProcess(jmeterHome).run(testPlanPath);

		return "Stored and opened JMeter test plan at " + testPlanPath;
	}

	@ShellMethod(key = { "jmeter upload" }, value = "Uploads a locally stored JMeter load test and potentially annotates it.")
	public String uploadLoadTest(String loadTestPath, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(value = { "--annotate", "-a" }, defaultValue = "false") boolean annotate) throws IOException {
		AppId aid = contextManager.getAppIdOrFail(appId);

		String error = initTestPlanWriter(propertiesProvider.getProperty(KEY_JMETER_HOME));

		if (!error.isEmpty()) {
			return error;
		}

		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		Path testPlanDir = Paths.get(workingDir).resolve(loadTestPath);

		File dir = testPlanDir.toFile();

		if (!dir.exists() || !dir.isDirectory()) {
			return testPlanDir.toAbsolutePath().toString() + " is not a directory!";
		}

		ListedHashTree testPlan = null;
		Map<String, String[][]> behaviors = new HashMap<>();

		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".jmx")) {
				testPlan = (ListedHashTree) SaveService.loadTree(file);
			} else if (file.getName().endsWith(".csv")) {
				behaviors.put(file.getName(), csvHandler.readValues(file.getAbsolutePath()));
			}
		}

		if (testPlan == null) {
			return "No .jmx test plan found!";
		}

		JMeterTestPlanBundle bundle = new JMeterTestPlanBundle(testPlan, behaviors);

		String continuityHost = propertiesProvider.getProperty(PropertiesProvider.KEY_URL);

		ResponseEntity<LinkExchangeModel> response = restTemplate.postForEntity(
				RestApi.JMeter.TestPlan.POST.viaOrchestrator().requestUrl("jmeter", aid).withHost(continuityHost).withQuery("annotate", Boolean.toString(annotate)).get(), bundle,
				LinkExchangeModel.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			return new StringBuilder().append("Successfully uploaded JMeter test plan with app-id ").append(aid).append(" at ").append(testPlanDir.toAbsolutePath().toString()).append("\n")
					.append(mapper.writeValueAsString(response.getBody())).toString();
		} else {
			return new StringBuilder().append("Could not upload JMeter test plan with app-id ").append(aid).append(" at ").append(testPlanDir.toAbsolutePath().toString()).append("\nResponse was: ")
					.append(response.getStatusCodeValue()).append(" - ").append(response.getStatusCode()).append("\n").append(response.getBody()).toString();
		}
	}

	private String initTestPlanWriter(String jmeterHome) {
		if (testPlanWriter == null) {
			if (jmeterHome == null) {
				return "Please set the jmeter home path first (call 'jmeter home [path]')";
			} else {
				testPlanWriter = new TestPlanWriter(jmeterHome);
			}
		} else if (jmeterHome == null) {
			return "Please set the jmeter home path first (call 'jmeter home [path]')";
		}

		return "";
	}

}
