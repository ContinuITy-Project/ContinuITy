package org.continuity.cli.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.api.entities.report.ApplicationChangeReport;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.api.rest.CustomHeaders;
import org.continuity.api.rest.RequestBuilder;
import org.continuity.api.rest.RestApi.Idpa;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.storage.IdpaStorage;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.commons.accesslogs.AccessLogEntry;
import org.continuity.commons.idpa.AnnotationExtractor;
import org.continuity.commons.idpa.AnnotationFromAccessLogsExtractor;
import org.continuity.commons.idpa.AnnotationValidityChecker;
import org.continuity.commons.idpa.ApplicationUpdater;
import org.continuity.commons.idpa.OpenApiToIdpaTransformer;
import org.continuity.commons.utils.FileUtils;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

/**
 * CLI for annotation handling.
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class IdpaCommands extends AbstractCommands {

	private static final String CONTEXT_NAME = "idpa";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("download", this, "downloadIdpa", String.class), //
			new Shorthand("open", this, "openIdpa", String.class), //
			new Shorthand("app", this, "goToIdpaAappContext", String.class), //
			new Shorthand("app upload", this, "uploadApplication", String.class), //
			new Shorthand("app create", this, "createIdpaApplication", String.class, String.class), //
			new Shorthand("app update", this, "updateIdpaApplication", String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class), //
			new Shorthand("app init", this, "initApplication", String.class), //
			new Shorthand("ann", this, "goToIdpaAnnContext", String.class), //
			new Shorthand("ann upload", this, "uploadAnnotation", String.class), //
			new Shorthand("ann init", this, "initAnnotation", String.class), //
			new Shorthand("ann extract", this, "extractAnnotation", String.class, String.class, String.class), //
			new Shorthand("ann check", this, "checkAnnotation", String.class) //
	);

	private static final String APP_CONTEXT_NAME = "app";

	private final CliContext appContext = new CliContext(APP_CONTEXT_NAME, //
			new Shorthand("upload", this, "uploadApplication", String.class), //
			new Shorthand("create", this, "createIdpaApplication", String.class, String.class), //
			new Shorthand("update", this, "updateIdpaApplication", String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class), //
			new Shorthand("open", this, "openIdpa", String.class), //
			new Shorthand("init", this, "initApplication", String.class) //
	);

	private static final String ANN_CONTEXT_NAME = "ann";

	private final CliContext annContext = new CliContext(ANN_CONTEXT_NAME, //
			new Shorthand("upload", this, "uploadAnnotation", String.class), //
			new Shorthand("init", this, "initAnnotation", String.class), //
			new Shorthand("extract", this, "extractAnnotation", String.class, String.class, String.class), //
			new Shorthand("check", this, "checkAnnotation", String.class), //
			new Shorthand("open", this, "openIdpa", String.class) //
	);

	private static final String PARAM_VERSION = "version";

	private PropertiesProvider propertiesProvider;

	private RestTemplate restTemplate;

	private ObjectMapper mapper;

	private CliContextManager contextManager;

	private IdpaStorage storage;

	private final OpenApiToIdpaTransformer openApiTransfomer = new OpenApiToIdpaTransformer();

	private final SwaggerParser swaggerParser = new SwaggerParser();

	private final ApplicationUpdater applicationUpdater = new ApplicationUpdater();

	@Autowired
	public IdpaCommands(PropertiesProvider propertiesProvider, RestTemplate restTemplate, ObjectMapper mapper, CliContextManager contextManager, IdpaStorage storage) {
		super(contextManager);
		this.propertiesProvider = propertiesProvider;
		this.restTemplate = restTemplate;
		this.mapper = mapper;
		this.contextManager = contextManager;
		this.storage = storage;
	}

	@ShellMethod(key = { CONTEXT_NAME }, value = "Goes to the 'idpa' context so that the shorthands can be used.")
	public AttributedString goToIdpaContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "idpa download" }, value = "Downloads and opens the IDPA with the specified app-id.")
	public AttributedString downloadIdpa(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Exception {
		return executeWithAppId(appId, (aid) -> {
			String url = WebUtils.addProtocolIfMissing(propertiesProvider.getProperty(PropertiesProvider.KEY_URL));

			ResponseEntity<Application> applicationResponse = restTemplate
					.getForEntity(Idpa.Application.GET.viaOrchestrator().requestUrl(aid).withHost(url).withQueryIfNotEmpty(PARAM_VERSION, contextManager.getCurrentVersion()).get(), Application.class);

			ResponseEntity<ApplicationAnnotation> annotationResponse = restTemplate.getForEntity(
					Idpa.Annotation.GET.viaOrchestrator().requestUrl(aid).withHost(url).withQueryIfNotEmpty(PARAM_VERSION, contextManager.getCurrentVersion()).get(), ApplicationAnnotation.class);

			storage.store(applicationResponse.getBody(), aid);
			storage.openApplication(aid);

			storage.store(annotationResponse.getBody(), aid);
			storage.openAnnotation(aid);

			ResponseBuilder response = new ResponseBuilder();
			List<String> brokenValues = annotationResponse.getHeaders().get(CustomHeaders.BROKEN);

			if ((brokenValues != null) && brokenValues.contains("true")) {
				response.error("The annotation is broken! Please use ").boldError("ann check").error(" for details.").newline();
			}

			return response.normal("Downloaded and opened the IDPA with app-id ").normal(aid).normal(" and version ").normal(contextManager.getCurrentVersionOrLatest()).normal(".").build();
		});
	}

	@ShellMethod(key = { "idpa open" }, value = "Opens an already downloaded IDPA with the specified app-id.")
	public AttributedString openIdpa(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Exception {
		return executeWithAppId(appId, (aid) -> {
			boolean appExists = storage.openApplication(aid);
			boolean annExists = storage.openAnnotation(aid);

			ResponseBuilder response = new ResponseBuilder();

			if (appExists) {
				response.normal("Opened the IDPA application model with app-id ").bold(aid);
			} else {
				response.normal("The IDPA application model with app-id ").bold(aid).normal(" does not exist.");
			}

			response.normal("\n");

			if (annExists) {
				response.normal("Opened the IDPA annotation with app-id ").bold(aid);
			} else {
				response.normal("The IDPA annotation with app-id ").bold(aid).normal(" does not exist.");
			}

			return response.build();
		});
	}

	@ShellMethod(key = { "idpa app" }, value = "Goes to the 'idpa/app' context so that the shorthands can be used.")
	public AttributedString goToIdpaAappContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context, appContext);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "idpa app init" }, value = "Initializes an application model with the specified app-id.")
	public AttributedString initApplication(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Exception {
		return executeWithAppId(appId, (aid) -> {
			if (storage.applicationExists(aid)) {
				return new ResponseBuilder().error("There is already an application model for app-id ").boldError(aid).error("!").build();
			} else {
				Application application = new Application();
				application.setId(aid.toString());
				application.setTimestamp(new Date());

				storage.store(application, aid);
				storage.openApplication(aid);

				return new ResponseBuilder().normal("Initialized and opened the annotation.").build();
			}
		});
	}

	@ShellMethod(key = { "idpa app create" }, value = "Creates an IDPA application model from an OpenApi.")
	public AttributedString createIdpaApplication(String openApiLocation, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId)
			throws Exception {
		return executeWithAppId(appId, (aid) -> {
			Swagger swagger = readSwagger(openApiLocation);
			Application application = openApiTransfomer.transform(swagger);
			application.setId(aid.toString());

			storage.store(application, aid);
			storage.openApplication(aid);

			return new ResponseBuilder().normal("Created and opened the application model with app-id ").bold(aid).normal(" from the OpenAPI at ").normal(openApiLocation).build();
		});
	}

	@ShellMethod(key = { "idpa app update" }, value = "Updates an IDPA application model from an OpenApi.")
	public AttributedString updateIdpaApplication(
			@ShellOption(help = "Location where the OpenAPI model can be found. Can be a URL or a file path. A file name can contain UNIX-like wildcards.") String openApiLocation,
			@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(defaultValue = "false", value = { "--add", "-a" }, help = "Consider element additions.") boolean add,
			@ShellOption(defaultValue = "false", value = { "--remove", "-r" }, help = "Consider element removals.") boolean remove,
			@ShellOption(defaultValue = "false", value = { "--change", "-c" }, help = "Consider element changes.") boolean change,
			@ShellOption(defaultValue = "false", value = { "--endpoints", "-e" }, help = "Consider endpoints.") boolean endpoints,
			@ShellOption(defaultValue = "false", value = { "--parameters", "-p" }, help = "Consider parameters.") boolean parameters,
			@ShellOption(defaultValue = "false", value = { "--hide-ignored" }, help = "Consider parameters.") boolean hideIgnored)
			throws Exception {

		return executeWithAppId(appId, (aid) -> {
			ResponseBuilder response = new ResponseBuilder();

			Application origApplication = storage.readApplication(aid);
			EnumSet<ApplicationChangeType> changeTypes = changeTypesFromBooleans(add, remove, change, endpoints, parameters);

			if (!openApiLocation.startsWith("http")) {
				for (File openApiFile : FileUtils.getAllFilesMatchingWildcards(openApiLocation)) {
					ApplicationChangeReport report = updateApplicationModel(origApplication, openApiFile.getPath(), aid, changeTypes);
					report.setUpdatedApplication(null);

					if (hideIgnored) {
						report.setIgnoredApplicationChanges(null);
					}

					response.normal("Updated with ").normal(openApiFile.getPath()).normal(":\n").normal(mapper.writeValueAsString(report));
				}
			} else {
				ApplicationChangeReport report = updateApplicationModel(origApplication, openApiLocation, aid, changeTypes);
				report.setUpdatedApplication(null);

				if (hideIgnored) {
					report.setIgnoredApplicationChanges(null);
				}

				response.normal("Updated with ").normal(openApiLocation).normal(":\n").normal(mapper.writeValueAsString(report));
			}

			return response.build();
		});
	}

	private ApplicationChangeReport updateApplicationModel(Application origApplication, String openApiLocation, AppId aid, EnumSet<ApplicationChangeType> changeTypes)
			throws JsonGenerationException, JsonMappingException, IOException {
		Swagger swagger = readSwagger(openApiLocation);

		Application newApplication = openApiTransfomer.transform(swagger);

		ApplicationChangeReport report = applicationUpdater.updateApplication(origApplication, newApplication, changeTypes);

		AppId origAid = AppId.fromString(aid + "_old");
		origApplication.setId(origAid.toString());

		storage.store(origApplication, origAid);
		storage.store(report.getUpdatedApplication(), aid);

		storage.openApplication(origAid);
		storage.openApplication(aid);

		return report;
	}

	@SuppressWarnings("unchecked")
	@ShellMethod(key = { "idpa app upload" }, value = "Uploads the application model with the specified app-id. Can break the online stored annotation!")
	public AttributedString uploadApplication(
			@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE, help = "App-id of the application model. Can contain UNIX-like wildcards.") String pattern)
			throws Exception {
		return executeWithAppId(pattern, aidPattern -> {
			List<AppId> aids = new ArrayList<>();
			String url = propertiesProvider.getProperty(PropertiesProvider.KEY_URL);
			ResponseBuilder responses = new ResponseBuilder();

			boolean error = storage.listApplications(aidPattern).map(pair -> {
				AppId aid = pair.getLeft();
				Application application = pair.getRight();
				ResponseEntity<String> response;

				aids.add(aid);
				try {
					response = restTemplate.postForEntity(Idpa.Application.UPDATE.viaOrchestrator().requestUrl(aid).withHost(url).get(), application, String.class);
				} catch (HttpStatusCodeException e) {
					response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
				}

				responses.newline().bold(aid);

				if (!response.getStatusCode().is2xxSuccessful()) {
					responses.error(" [ERROR]");
				}

				responses.newline();

				List<String> broken = null;

				try {
					broken = restTemplate.getForObject(
							Idpa.Annotation.GET_BROKEN.viaOrchestrator().requestUrl(aid).withQuery(PARAM_VERSION, application.getVersionOrTimestamp().toString()).withHost(url).get(), List.class);
				} catch (HttpStatusCodeException e) {
					responses.error("Error when checking broken annotations! ").boldError(e.getStatusCode()).error(" (").error(e.getStatusCode().getReasonPhrase()).error(") - ")
							.error(e.getResponseBodyAsString()).newline();
				}

				if ((broken != null) && (broken.size() > 0)) {
					responses.error("The new application version broke the following annotations: ");
					responses.error(broken.stream().collect(Collectors.joining(", "))).newline();
				}

				if (response.getStatusCode().is2xxSuccessful()) {
					responses.jsonAsYamlNormal(response.getBody());
					return false;
				} else {
					responses.jsonAsYamlError(response.getBody());
					return true;
				}
			}).reduce(Boolean::logicalOr).orElse(false);

			if (error) {
				return new ResponseBuilder().normal("Uploaded application models for app-ids ").normal(aids).normal(". ").error("Some of them resulted in errors:").newline().append(responses).build();
			} else {
				return new ResponseBuilder().normal("Successfully uploaded application models for app-ids ").normal(aids).normal(":").newline().append(responses).build();
			}
		});
	}

	@ShellMethod(key = { "idpa ann" }, value = "Goes to the 'idpa/ann' context so that the shorthands can be used.")
	public AttributedString goToIdpaAnnContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context, annContext);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "idpa ann upload" }, value = "Uploads the annotation with the specified app-id.")
	public AttributedString uploadAnnotation(
			@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE, help = "App-ids of the annotation. Can contain UNIX-like wildcards.") String pattern)
			throws Exception {
		return executeWithAppId(pattern, aidPattern -> {
			ResponseBuilder resp = new ResponseBuilder();

			List<AppId> aids = new ArrayList<>();
			ResponseBuilder responses = new ResponseBuilder();
			String url = propertiesProvider.getProperty(PropertiesProvider.KEY_URL);

			String currVersionStr = contextManager.getCurrentVersion();

			if (currVersionStr == null) {
				currVersionStr = ApiFormats.DATE_FORMAT.format(new Date());
				resp.normal("No version set! Using the current time as fallback: ").bold(currVersionStr).newline();
			}

			VersionOrTimestamp currVersion = VersionOrTimestamp.fromString(currVersionStr);

			boolean error = storage.listAnnotations(aidPattern).map(pair -> {
				AppId aid = pair.getLeft();
				ApplicationAnnotation annotation = pair.getRight();

				aids.add(aid);

				RequestBuilder req = Idpa.Annotation.UPDATE.viaOrchestrator().requestUrl(aid).withHost(url);

				if (annotation.getVersionOrTimestamp().isEmpty()) {
					resp.bold("Annotation '").normal(aid).bold("' has no version! Setting the current one as fallback: ").normal(currVersion).newline();
					annotation.setVersionOrTimestamp(currVersion);
				}

				ResponseEntity<String> response;
				try {
					response = restTemplate.postForEntity(req.get(), annotation, String.class);
				} catch (HttpStatusCodeException e) {
					response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
				}

				responses.newline().bold(aid);

				if (response.getStatusCode().is2xxSuccessful()) {
					responses.newline().jsonAsYamlNormal(response.getBody());
					return false;
				} else {
					responses.error(" [ERROR]").newline().jsonAsYamlError(response.getBody());
					return true;
				}
			}).reduce(Boolean::logicalOr).orElse(false);

			if (error) {
				return resp.normal("Uploaded annotations for app-ids ").normal(aids).normal(". ").error("Some of them resulted in errors:").newline().append(responses).build();
			} else {
				return resp.normal("Successfully uploaded annotations for app-ids ").normal(aids).normal(":").newline().append(responses).build();
			}
		});
	}

	@ShellMethod(key = { "idpa ann init" }, value = "Initializes an annotation for the stored application model with the specified app-id.")
	public AttributedString initAnnotation(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Exception {
		return executeWithAppId(appId, (aid) -> {
			Application application = storage.readApplication(aid);
			ApplicationAnnotation annotation = storage.readAnnotation(aid);

			annotation = new AnnotationExtractor().extractAnnotation(application);

			storage.storeIfNotPresent(annotation, aid);

			storage.openApplication(aid);
			storage.openAnnotation(aid);

			return new ResponseBuilder().normal("Initialized and opened the annotation.").build();
		});
	}

	@ShellMethod(key = { "idpa ann extract" }, value = "Extracts an annotation for the stored application model with the specified app-id from Apache request logs.")
	public AttributedString extractAnnotation(String logsFile, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(defaultValue = AccessLogEntry.DEFAULT_REGEX, help = "The regular expression used to extract the request method and path including the query. There should be one capture group per property in the mentioned order.") String regex)
			throws Exception {
		return executeWithAppId(appId, (aid) -> {
			Application application = storage.readApplication(aid);
			ApplicationAnnotation annotation = storage.readAnnotation(aid);

			Path pathToLogs = Paths.get(logsFile);
			Path workingDir = Paths.get(propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR));

			if (pathToLogs.isAbsolute()) {
				pathToLogs = workingDir.resolve(pathToLogs);
			}

			AnnotationFromAccessLogsExtractor extractor = new AnnotationFromAccessLogsExtractor(application, pathToLogs, workingDir);
			extractor.setRegex(regex);
			extractor.extract();

			annotation = extractor.getExtractedAnnotation();
			Application filteredApplication = extractor.getFilteredApplication();
			Set<String> ignoredRequests = extractor.getIgnoredRequests();

			storage.storeIfNotPresent(annotation, aid);
			storage.openAnnotation(aid);

			storage.store(filteredApplication, AppId.fromString(aid + "-filtered"));
			storage.openApplication(AppId.fromString(aid + "-filtered"));

			return new ResponseBuilder().normal("Extracted and opened the annotation and the filtered application model.").newline().normal("Ignored requests:").newline()
					.normal(ignoredRequests.stream().collect(Collectors.joining("\n"))).build();
		});
	}

	@ShellMethod(key = { "idpa ann check" }, value = "Checks whether the annotation with the specified app-id fits to the respective application model.")
	public AttributedString checkAnnotation(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Exception {
		return executeWithAppId(appId, (aid) -> {
			Application application = storage.readApplication(aid);
			ApplicationAnnotation annotation = storage.readAnnotation(aid);

			AnnotationValidityChecker checker = new AnnotationValidityChecker(application);
			checker.checkAnnotation(annotation);
			AnnotationValidityReport report = checker.getReport();

			return new ResponseBuilder().normal(mapper.writeValueAsString(report)).build();
		});
	}

	private Swagger readSwagger(String openApiLocation) {
		Swagger swagger = swaggerParser.read(openApiLocation);

		if (swagger == null) {
			throw new IllegalArgumentException("The OpenAPI at location " + openApiLocation + " could not be found!");
		}

		return swagger;
	}

	private EnumSet<ApplicationChangeType> changeTypesFromBooleans(boolean add, boolean remove, boolean change, boolean endpoints, boolean parameters) {
		EnumSet<ApplicationChangeType> set = EnumSet.noneOf(ApplicationChangeType.class);

		boolean noEntity = !endpoints && !parameters;
		boolean noOperation = !add && !remove && !change;

		change |= noOperation;
		remove |= noOperation;
		add |= noOperation;

		endpoints |= noEntity;
		parameters |= noEntity;

		if (change && endpoints) {
			set.add(ApplicationChangeType.ENDPOINT_CHANGED);
		}

		if (remove && endpoints) {
			set.add(ApplicationChangeType.ENDPOINT_REMOVED);
		}

		if (add && endpoints) {
			set.add(ApplicationChangeType.ENDPOINT_ADDED);
		}

		if (change && parameters) {
			set.add(ApplicationChangeType.PARAMETER_CHANGED);
		}

		if (remove && parameters) {
			set.add(ApplicationChangeType.PARAMETER_REMOVED);
		}

		if (add && parameters) {
			set.add(ApplicationChangeType.PARAMETER_ADDED);
		}

		return set;
	}

}
