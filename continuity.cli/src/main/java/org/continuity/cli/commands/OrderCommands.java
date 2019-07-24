package org.continuity.cli.commands;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.links.SessionsStatus;
import org.continuity.api.entities.order.LoadTestType;
import org.continuity.api.entities.order.Order;
import org.continuity.api.entities.order.OrderGoal;
import org.continuity.api.entities.order.OrderMode;
import org.continuity.api.entities.order.OrderOptions;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.entities.order.TailoringApproach;
import org.continuity.api.entities.order.WorkloadModelType;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.entities.report.OrderResponse;
import org.continuity.api.rest.RestApi;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.storage.OrderStorage;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.commons.utils.WebUtils;
import org.continuity.dsl.description.ContextParameter;
import org.continuity.dsl.description.ForecastInput;
import org.continuity.dsl.description.ForecastOptions;
import org.continuity.dsl.description.IntensityCalculationInterval;
import org.continuity.dsl.description.Measurement;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class OrderCommands {

	private static final String ORDERS_DIR = "order";

	private static final String NEW_ORDER_FILENAME = "order.yml";

	private static final String CONTEXT_NAME = "order";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("create", this, "createOrder", String.class), //
			new Shorthand("edit", this, "editOrder", String.class), //
			new Shorthand("submit", this, "submitOrder"), //
			new Shorthand("wait", this, "waitForOrder", String.class, String.class), //
			new Shorthand("report", this, "getOrderReport", String.class), //
			new Shorthand("clean", this, "cleanOrders") //
	);

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

	@ShellMethod(key = { CONTEXT_NAME }, value = "Goes to the 'order' context so that the shorthands can be used.")
	public AttributedString goToIdpaContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "order create" }, value = "Creates a new order.")
	public String createOrder(@ShellOption(defaultValue = "create-load-test") String goal) throws JsonGenerationException, JsonMappingException, IOException {
		OrderGoal orderGoal = OrderGoal.fromPrettyString(goal);

		if (orderGoal == null) {
			return "Unknown order goal " + goal + "! The allowed goals are " + Arrays.stream(OrderGoal.values()).map(OrderGoal::toPrettyString).reduce((a, b) -> a + ", " + b).get();
		}

		Order order = initializeOrder();
		order.setGoal(orderGoal);

		Desktop.getDesktop().open(storeAsNewOrder(order));

		return "Created and opened a new order at orders/order-new.yml";
	}

	@ShellMethod(key = { "order edit" }, value = "Edits an already created order.")
	public String editOrder(@ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws JsonGenerationException, JsonMappingException, IOException {
		Order order = storage.getOrder(id);

		if (order == null) {
			return "There is no order with ID " + id;
		}

		Desktop.getDesktop().open(storeAsNewOrder(order));

		return "Updated the current order and opened it.";
	}

	@ShellMethod(key = { "order submit" }, value = "Submits the latest created order.")
	public String submitOrder() throws JsonParseException, JsonMappingException, IOException {
		Path orderDir = Paths.get(propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR), ORDERS_DIR, NEW_ORDER_FILENAME);
		Order order = mapper.readValue(orderDir.toFile(), Order.class);

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.getProperty(PropertiesProvider.KEY_URL));
		ResponseEntity<OrderResponse> response;
		try {
			response = restTemplate.postForEntity(RestApi.Orchestrator.Orchestration.SUBMIT.requestUrl().withHost(url).get(), order, OrderResponse.class);
		} catch (HttpStatusCodeException e) {
			return e.getResponseBodyAsString();
		}

		String storageId = storage.newOrder(order);
		storage.storeLinks(storageId, response.getBody());

		StringBuilder message = new StringBuilder();

		message.append("Submitted the order, storage ID is ");
		message.append(storageId);
		message.append(". For further actions:\n");
		message.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody()));

		return message.toString();
	}

	@ShellMethod(key = { "order wait" }, value = "Waits for an order to be finished.")
	public String waitForOrder(@ShellOption(defaultValue = "1000") String timeout, @ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws IOException {
		OrderResponse tuple = storage.getLinks(id);

		if (tuple == null) {
			return "Please create and submit an order before waiting!";
		}

		ResponseEntity<OrderReport> response;
		try {
			response = restTemplate.getForEntity(tuple.getWaitLink() + "?timeout=" + timeout, OrderReport.class);
		} catch (HttpStatusCodeException e) {
			return e.getResponseBodyAsString();
		}

		if (response.getStatusCode().equals(HttpStatus.OK) && response.hasBody()) {
			storage.storeReport(id, response.getBody());

			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody());
		} else {
			return "The order is not finished, yet.";
		}
	}

	@ShellMethod(key = { "order report" }, value = "Gets the order report if available.")
	public String getOrderReport(@ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws IOException {
		OrderReport report = storage.getReport(id);

		if (report == null) {
			return waitForOrder("0", id);
		} else {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
		}
	}

	@ShellMethod(key = { "order clean" }, value = "Cleans the order storage.")
	public String cleanOrders() throws IOException {
		int num = storage.clean();

		return "Cleaned " + num + " orders.";
	}

	private Order initializeOrder() {
		Order order = new Order();

		order.setMode(OrderMode.PAST_SESSIONS);

		if (contextManager.getCurrentAppId() == null) {
			order.setAppId(AppId.fromString("APP_ID"));
		} else {
			order.setAppId(contextManager.getCurrentAppId());
		}

		order.setServices(Arrays.asList(ServiceSpecification.fromString("SERVICE_OVERRIDING_APP_ID")));

		String version;

		if (contextManager.getCurrentVersion() == null) {
			version = "v0.0.0";
		} else {
			version = contextManager.getCurrentVersion();
		}

		try {
			order.setVersion(VersionOrTimestamp.fromString(version));
		} catch (NumberFormatException | ParseException e) {
			e.printStackTrace();
		}

		OrderOptions options = new OrderOptions();
		options.setDuration(60);
		options.setNumUsers(1);
		options.setRampup(1);
		options.setLoadTestType(LoadTestType.JMETER);
		options.setWorkloadModelType(WorkloadModelType.WESSBAS);
		options.setIntensityCalculationInterval(IntensityCalculationInterval.MINUTE);
		options.setTailoringApproach(TailoringApproach.LOG_BASED);
		order.setOptions(options);

		Measurement measurement = new Measurement("Name of measurement containing contextual data");
		List<ContextParameter> covariates = new LinkedList<ContextParameter>();
		covariates.add(measurement);
		ForecastOptions forecastOpt = new ForecastOptions("2019/01/01 00:00:00", IntensityCalculationInterval.HOUR, "telescope or prophet", "http://localhost:8086");
		ForecastInput forecastInput = new ForecastInput(covariates, forecastOpt);
		order.setForecastInput(forecastInput);

		LinkExchangeModel links = new LinkExchangeModel();
		links.getTraceLinks().setFrom(DateUtils.addHours(new Date(), 1)).setTo(new Date());
		links.getSessionLogsLinks().setSimpleLink("SIMPLE_SESSION_LOGS_LINK");
		links.getSessionLogsLinks().setExtendedLink("EXTENDED_SESSION_LOGS_LINK");
		links.getSessionsBundlesLinks().setLink("SESSIONS_BUNDLES_LINKS").setStatus(SessionsStatus.NOT_CHANGED);
		links.getForecastLinks().setLink("FORECAST_LINKS");
		links.getWorkloadModelLinks().setType(WorkloadModelType.WESSBAS).setLink("WORKLOAD_MODEL_LINK");
		links.getLoadTestLinks().setType(LoadTestType.JMETER).setLink("LOADTEST_LINK");
		order.setSource(links);

		return order;
	}

	private File storeAsNewOrder(Order order) throws JsonGenerationException, JsonMappingException, IOException {
		Path ordersDir = Paths.get(propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR), ORDERS_DIR);
		ordersDir.toFile().mkdirs();

		File orderFile = ordersDir.resolve(NEW_ORDER_FILENAME).toFile();
		mapper.writeValue(orderFile, order);

		return orderFile;
	}

}
