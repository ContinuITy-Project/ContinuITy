package org.continuity.cli.commands;

import java.nio.file.Path;

import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.order.LoadTestType;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestEndpoint;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.exception.CliException;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.storage.JMeterStorage;
import org.continuity.cli.storage.OrderStorage;
import org.continuity.cli.utils.ResponseBuilder;
import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz
 *
 */
@ShellComponent
@ShellCommandGroup("JMeter Commands")
public class JMeterCommands extends AbstractCommands {

	private static final String CONTEXT_NAME = "jmeter";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("home", this, "setJMeterHome", String.class), //
			new Shorthand("download", this, "downloadLoadTest", String.class, boolean.class), //
			new Shorthand("upload", this, "uploadLoadTest", String.class, String.class, boolean.class), //
			new Shorthand("open", this, "openLoadTest", String.class) //
	);

	private final PropertiesProvider propertiesProvider;

	private final RestTemplate restTemplate;

	private final JMeterStorage storage;

	private final OrderStorage orderStorage;

	private final CliContextManager contextManager;

	private final ObjectMapper mapper;

	@Autowired
	public JMeterCommands(PropertiesProvider propertiesProvider, RestTemplate restTemplate, JMeterStorage storage, OrderStorage orderStorage, CliContextManager contextManager, ObjectMapper mapper) {
		super(contextManager);

		this.propertiesProvider = propertiesProvider;
		this.restTemplate = restTemplate;
		this.storage = storage;
		this.orderStorage = orderStorage;
		this.contextManager = contextManager;
		this.mapper = mapper;
	}

	@ShellMethod(key = { CONTEXT_NAME }, value = "Goes to the 'jmeter' context so that the shorthands can be used.")
	public AttributedString goToJmeterContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "jmeter home" }, value = "Sets the home directory of JMeter (where the bin directory is placed).")
	public String setJMeterHome(String jmeterHome) throws CliException {
		jmeterHome = jmeterHome.replace("\\", "/");
		Object old = propertiesProvider.putProperty(JMeterStorage.KEY_JMETER_HOME, jmeterHome);
		storage.init();
		return old == null ? "Set JMeter home." : "Replaced old JMeter home: " + old;
	}

	@ShellMethod(key = { "jmeter download" }, value = "Downloads and opens a JMeter load test specified by a link.")
	public AttributedString downloadLoadTest(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String loadTestLink,
			@ShellOption(help = "Indicates that the downloaded test should not be opened.") boolean silent) throws Exception {
		return executeWithCurrentAppId((aid) -> {
			storage.init();

			String link = loadTestLink;

			if (Shorthand.DEFAULT_VALUE.equals(link)) {
				OrderReport report = orderStorage.readLatestReport(aid);

				if ((report == null) || (report.getArtifacts() == null) || (report.getArtifacts().getLoadTestLinks().getLink() == null)) {
					return new ResponseBuilder().error("Cannot download the JMeter test of the latest order. The link is missing!").build();
				} else if (report.getArtifacts().getLoadTestLinks().getType() != LoadTestType.JMETER) {
					return new ResponseBuilder().error("Cannot download the JMeter test of the latest order. The link points to a ")
							.boldError(report.getArtifacts().getLoadTestLinks().getType().toPrettyString()).error(" test!").build();
				} else {
					link = report.getArtifacts().getLoadTestLinks().getLink();
				}
			}

			ResponseEntity<JMeterTestPlanBundle> response = restTemplate.getForEntity(RestEndpoint.urlViaOrchestrator(link, propertiesProvider.getProperty(PropertiesProvider.KEY_URL)),
					JMeterTestPlanBundle.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				return new ResponseBuilder().error(response.toString()).build();
			}

			Path testPlanPath = silent ? storage.store(response.getBody(), aid, link) : storage.storeAndOpen(response.getBody(), aid, link);

			return new ResponseBuilder().normal("Stored and opened JMeter test plan at ").normal(testPlanPath).build();
		});
	}

	@ShellMethod(key = { "jmeter open" }, value = "Opens an already downloaded JMeter load test specified by a link.")
	public AttributedString openLoadTest(@ShellOption(defaultValue = JMeterStorage.LINK_LATEST) String link) throws Exception {
		return executeWithCurrentAppId(aid -> {
			storage.init();

			Path testPlanPath = storage.open(aid, link);

			return new ResponseBuilder().normal("Stored and opened JMeter test plan at ").normal(testPlanPath).build();
		});
	}

	@ShellMethod(key = { "jmeter upload" }, value = "Uploads a locally stored JMeter load test and potentially annotates it.")
	public AttributedString uploadLoadTest(String loadTestPath, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(value = { "--annotate", "-a" }, defaultValue = "false") boolean annotate) throws Exception {
		return executeWithAppId(appId, (aid) -> {
			storage.init();

			JMeterTestPlanBundle bundle = storage.read(loadTestPath);

			String continuityHost = propertiesProvider.getProperty(PropertiesProvider.KEY_URL);

			ResponseEntity<ArtifactExchangeModel> response = restTemplate.postForEntity(
					RestApi.JMeter.TestPlan.POST.viaOrchestrator().requestUrl("jmeter", aid).withHost(continuityHost).withQuery("annotate", Boolean.toString(annotate)).get(), bundle,
					ArtifactExchangeModel.class);

			if (response.getStatusCode().is2xxSuccessful()) {
				return new ResponseBuilder().normal("Successfully uploaded JMeter test plan with app-id ").bold(aid).normal(" at ").normal(loadTestPath).normal("\n")
						.normal(mapper.writeValueAsString(response.getBody())).build();
			} else {
				return new ResponseBuilder().error("Could not upload JMeter test plan with app-id ").boldError(aid).error(" at ").error(loadTestPath).error("\nResponse was: ")
						.boldError(response.getStatusCodeValue()).boldError(" - ").boldError(response.getStatusCode().getReasonPhrase()).error("\n").error(response.getBody()).build();
			}
		});
	}

}
