package org.continuity.cli.commands;

import java.awt.Desktop;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.continuity.dsl.StringOrNumeric;
import org.continuity.dsl.adjustment.IntensityIncreasedAdjustment;
import org.continuity.dsl.adjustment.IntensityMultipliedAdjustment;
import org.continuity.dsl.context.Context;
import org.continuity.dsl.context.TimeSpecification;
import org.continuity.dsl.context.WorkloadAdjustment;
import org.continuity.dsl.context.WorkloadInfluence;
import org.continuity.dsl.context.influence.FixedInfluence;
import org.continuity.dsl.context.influence.IncreasedInfluence;
import org.continuity.dsl.context.influence.IsAbsentInfluence;
import org.continuity.dsl.context.influence.MultipliedInfluence;
import org.continuity.dsl.context.influence.OccursInfluence;
import org.continuity.dsl.context.timespec.AbsentSpecification;
import org.continuity.dsl.context.timespec.AfterSpecification;
import org.continuity.dsl.context.timespec.BeforeSpecification;
import org.continuity.dsl.context.timespec.PlusSpecification;
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
			new Shorthand("wait", this, "waitForOrder", String.class, String.class), //
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
	public AttributedString createOrder(@ShellOption(defaultValue = "create-load-test") String goal) throws Exception {
		return executeWithCurrentAppId((aid) -> {
			OrderGoal orderGoal = OrderGoal.fromPrettyString(goal);

			if (orderGoal == null) {
				return new ResponseBuilder().error("Unknown order goal ").boldError(goal).error("! The allowed goals are ")
						.error(Arrays.stream(OrderGoal.values()).map(OrderGoal::toPrettyString).reduce((a, b) -> a + ", " + b).get()).build();
			}

			Order order = initializeOrder();
			order.setGoal(orderGoal);

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
	public AttributedString waitForOrder(@ShellOption(defaultValue = "1000") String timeout, @ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws Exception {
		return executeWithCurrentAppId((aid) -> {
			OrderResponse orderResponse = storage.readResponse(aid, id);

			if (orderResponse == null) {
				return new ResponseBuilder().error("Please create and submit an order before waiting!").build();
			}

			ResponseEntity<OrderReport> response;
			try {
				response = restTemplate.getForEntity(orderResponse.getWaitLink() + "?timeout=" + timeout, OrderReport.class);
			} catch (HttpStatusCodeException e) {
				return new ResponseBuilder().error(e.getResponseBodyAsString()).build();
			}

			if (response.getStatusCode().equals(HttpStatus.OK) && response.hasBody()) {
				storage.store(aid, response.getBody());

				return new ResponseBuilder().normal(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody())).build();
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
				return waitForOrder("0", id);
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

		order.setMode(OrderMode.PAST_WORKLOAD);

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
		options.setDuration(60);
		options.setNumUsers(1);
		options.setRampup(1);
		options.setLoadTestType(LoadTestType.JMETER);
		options.setWorkloadModelType(WorkloadModelType.WESSBAS);
		options.setTailoringApproach(TailoringApproach.LOG_BASED);
		options.setForecastApproach("Telescope");
		order.setOptions(options);

		order.setContext(createContext());

		return order;
	}

	private Context createContext() {
		Context context = new Context();

		List<TimeSpecification> when = new ArrayList<>();

		AbsentSpecification absent = new AbsentSpecification();
		absent.setWhat("black-friday");
		when.add(absent);

		AfterSpecification after = new AfterSpecification();
		after.setDate(new Date());
		when.add(after);

		PlusSpecification plus = new PlusSpecification();
		plus.setDuration(Duration.ofHours(1));
		when.add(plus);

		context.setWhen(when);

		Map<String, List<WorkloadInfluence>> influences = new HashMap<>();
		FixedInfluence fixed = new FixedInfluence();
		fixed.setValue(new StringOrNumeric("sunny"));
		influences.put("weather", Collections.singletonList(fixed));

		MultipliedInfluence multiplied = new MultipliedInfluence();
		multiplied.setWith(1.3);
		IncreasedInfluence increased = new IncreasedInfluence();
		increased.setBy(5);
		influences.put("temperature", Arrays.asList(multiplied, increased));

		OccursInfluence occurs = new OccursInfluence();
		IsAbsentInfluence isAbsent = new IsAbsentInfluence();
		BeforeSpecification absentBefore = new BeforeSpecification();
		absentBefore.setDate(new Date());
		isAbsent.setWhen(Collections.singletonList(absentBefore));
		influences.put("outage", Arrays.asList(occurs, isAbsent));

		context.setInfluencing(influences);

		List<WorkloadAdjustment> adjustments = new ArrayList<>();
		IntensityMultipliedAdjustment intMultiplied = new IntensityMultipliedAdjustment();
		intMultiplied.setWith(1.5);
		adjustments.add(intMultiplied);

		IntensityIncreasedAdjustment intIncreased = new IntensityIncreasedAdjustment();
		intIncreased.setBy(200);
		intIncreased.setGroup(2);
		adjustments.add(intIncreased);

		context.setAdjusted(adjustments);

		return context;
	}

}
