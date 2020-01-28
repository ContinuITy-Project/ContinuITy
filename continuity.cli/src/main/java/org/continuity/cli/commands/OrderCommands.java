package org.continuity.cli.commands;

import java.awt.Desktop;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.continuity.api.entities.exchange.ArtifactType;
import org.continuity.api.entities.order.Order;
import org.continuity.api.entities.order.OrderOptions;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.entities.order.TailoringApproach;
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
import org.continuity.dsl.ContextValue;
import org.continuity.dsl.WorkloadDescription;
import org.continuity.dsl.elements.ContextSpecification;
import org.continuity.dsl.elements.TimeSpecification;
import org.continuity.dsl.elements.TypedProperties;
import org.continuity.dsl.elements.timeframe.Condition;
import org.continuity.dsl.elements.timeframe.ConditionalTimespec;
import org.continuity.dsl.elements.timeframe.Timerange;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class OrderCommands extends AbstractCommands {

	private static final String CONTEXT_NAME = "order";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("create", this, "createOrder", String.class), //
			new Shorthand("edit", this, "editOrder", String.class), //
			new Shorthand("submit", this, "submitOrder"), //
			new Shorthand("wait", this, "waitForOrder", String.class, String.class, boolean.class), //
			new Shorthand("report", this, "getOrderReport", String.class), //
			new Shorthand("clean", this, "cleanOrders", boolean.class) //
	);

	private PropertiesProvider propertiesProvider;

	private RestTemplate restTemplate;

	private OrderStorage storage;

	private CliContextManager contextManager;

	private ObjectMapper mapper;

	@Autowired
	public OrderCommands(PropertiesProvider propertiesProvider, RestTemplate restTemplate, OrderStorage storage, CliContextManager contextManager,
			ObjectMapper mapper) {
		super(contextManager);
		this.propertiesProvider = propertiesProvider;
		this.restTemplate = restTemplate;
		this.storage = storage;
		this.contextManager = contextManager;
		this.mapper = mapper;
	}

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
	public AttributedString createOrder(@ShellOption(defaultValue = "load-test") String target) throws Exception {
		return executeWithCurrentAppId((aid) -> {
			ArtifactType orderTarget = ArtifactType.fromPrettyString(target);

			if (orderTarget == null) {
				return new ResponseBuilder().error("Unknown order target ").boldError(target).error("! The allowed targets are ")
						.error(Arrays.stream(ArtifactType.values()).map(ArtifactType::toPrettyString).reduce((a, b) -> a + ", " + b).get()).build();
			}

			Order order = initializeOrder();
			order.setTarget(orderTarget);

			Desktop.getDesktop().open(storage.storeAsNew(contextManager.getCurrentAppId(), order).toFile());

			return new ResponseBuilder().normal("Created and opened a new order.").build();
		});
	}

	@ShellMethod(key = { "order edit" }, value = "Edits an already created order.")
	public AttributedString editOrder(@ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws Exception {
		return executeWithCurrentAppId((aid) -> {
			Order order = storage.readOrder(aid, id);

			if (order == null) {
				return new ResponseBuilder().error("There is no such order to be edited!").build();
			}

			Desktop.getDesktop().open(storage.storeAsNew(aid, order).toFile());

			return new ResponseBuilder().normal("Copied the order and opened it.").build();
		});
	}

	@ShellMethod(key = { "order submit" }, value = "Submits the latest created order.")
	public AttributedString submitOrder() throws Exception {
		return executeWithCurrentAppId((aid) -> {
			Order order = storage.readNew(aid);

			if (order == null) {
				order = storage.readLatestOrder(aid);

				if (order == null) {
					return new ResponseBuilder().error("There is no order to be submitted! Create one first using ").boldError("order create").build();
				}

				storage.storeAsNew(aid, order).toFile();
			}

			String url = WebUtils.addProtocolIfMissing(propertiesProvider.getProperty(PropertiesProvider.KEY_URL));
			ResponseEntity<OrderResponse> response;
			try {
				response = restTemplate.postForEntity(RestApi.Orchestrator.Orchestration.SUBMIT.requestUrl().withHost(url).get(), order, OrderResponse.class);
			} catch (HttpStatusCodeException e) {
				return new ResponseBuilder().error(e.getResponseBodyAsString()).build();
			}

			String orderId = storage.store(aid, response.getBody());
			storage.moveNew(aid, orderId);

			ResponseBuilder message = new ResponseBuilder();

			message.normal("Submitted the order, order ID is ");
			message.bold(orderId);
			message.normal(". For further actions:\n");
			message.normal(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody()));

			return message.build();
		});
	}

	@ShellMethod(key = { "order wait" }, value = "Waits for an order to be finished.")
	public AttributedString waitForOrder(@ShellOption(defaultValue = "1") String timeout, @ShellOption(defaultValue = OrderStorage.ID_LATEST) String id, boolean retry) throws Exception {
		return executeWithCurrentAppId((aid) -> {
			OrderResponse orderResponse = storage.readResponse(aid, id);
			long lTimeout = Long.parseLong(timeout);

			if (orderResponse == null) {
				return new ResponseBuilder().error("Please create and submit an order before waiting!").build();
			}

			ResponseEntity<OrderReport> response;
			try {
				response = restTemplate.getForEntity(orderResponse.getWaitLink() + "?timeout=" + (lTimeout * 1000), OrderReport.class);
			} catch (HttpStatusCodeException e) {
				return new ResponseBuilder().error(e.getResponseBodyAsString()).build();
			}

			if (response.getStatusCode().equals(HttpStatus.OK) && response.hasBody()) {
				storage.store(aid, response.getBody());

				return new ResponseBuilder().normal(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody())).build();
			} else if (retry) {
				System.out.println("The order is not finished, yet. Waiting and retrying...");
				Thread.sleep(2 * lTimeout * 1000);
				return waitForOrder(Long.toString(2 * lTimeout), id, true);
			} else {
				return new ResponseBuilder().bold("The order is not finished, yet.").build();
			}
		});
	}

	@ShellMethod(key = { "order report" }, value = "Gets the order report if available.")
	public AttributedString getOrderReport(@ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws Exception {
		return executeWithCurrentAppId((aid) -> {
			OrderReport report = storage.readReport(aid, id);

			if (report == null) {
				return waitForOrder("0", id, false);
			} else {
				return new ResponseBuilder().normal(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report)).build();
			}
		});
	}

	@ShellMethod(key = { "order clean" }, value = "Cleans the order storage.")
	public AttributedString cleanOrders(@ShellOption(value = { "--current", "-c" }, defaultValue = "false") boolean cleanCurrent) throws Exception {
		return executeWithCurrentAppId((aid) -> {
			int num = storage.clean(aid, cleanCurrent);

			return new ResponseBuilder().normal("Deleted ").bold(num).normal(" orders.").build();
		});
	}

	private Order initializeOrder() {
		Order order = new Order();

		if (contextManager.getCurrentAppId() == null) {
			order.setAppId(AppId.fromString("APP_ID"));
		} else {
			order.setAppId(contextManager.getCurrentAppId());
		}

		order.setServices(Arrays.asList(ServiceSpecification.fromString("SERVICE_OVERRIDING_APP_ID")));

		String version = contextManager.getCurrentVersion();

		if (version == null) {
			version = "v0.0.0";
		}

		try {
			order.setVersion(VersionOrTimestamp.fromString(version));
		} catch (NumberFormatException | ParseException e) {
			e.printStackTrace();
		}

		OrderOptions options = new OrderOptions();
		options.setTailoringApproach(TailoringApproach.LOG_BASED);
		options.setForecastApproach("Telescope");

		Map<ArtifactType, String> producers = new HashMap<>();
		producers.put(ArtifactType.WORKLOAD_MODEL, "wessbas");
		producers.put(ArtifactType.BEHAVIOR_MODEL, "wessbas");
		producers.put(ArtifactType.LOAD_TEST, "jmeter");
		options.setProducers(producers);

		order.setOptions(options);

		order.setWorkloadDescription(createWorkloadDescription());

		return order;
	}

	private WorkloadDescription createWorkloadDescription() {
		WorkloadDescription description = new WorkloadDescription();

		List<TimeSpecification> timeframe = new ArrayList<>();

		Timerange timerange = new Timerange().setFrom(LocalDateTime.now()).setDuration(Duration.ofDays(14));
		timeframe.add(timerange);

		ConditionalTimespec conditional = new ConditionalTimespec();
		conditional.getConditions().put("weather", new Condition().setIs(new ContextValue("sunny")));
		timeframe.add(conditional);

		description.setTimeframe(timeframe);

		Map<String, List<ContextSpecification>> context = new HashMap<>();

		context.put("temperature", Collections.singletonList(new ContextSpecification().setMultiplied(1.3).setAdded(5.0)));

		Timerange outageRange = new Timerange().setFrom(LocalDateTime.now().plus(Duration.ofHours(72))).setTo(LocalDateTime.now().plus(Duration.ofHours(74)));
		context.put("outage", Collections.singletonList(new ContextSpecification().setIs(new ContextValue(true)).setDuring(Collections.singletonList(outageRange))));

		description.setContext(context);

		description.setAggregation(new TypedProperties().setType("maximum"));

		Map<String, Object> adjustmentProps = new HashMap<>();
		adjustmentProps.put("amount", 200);
		adjustmentProps.put("group", 2);
		description.setAdjustments(Collections.singletonList(new TypedProperties().setType("users-added").setProperties(adjustmentProps)));

		return description;
	}

}
