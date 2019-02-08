package org.continuity.orchestrator.controllers;

import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.ROOT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.RESULT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.SUBMIT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.WAIT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.LoadTestType;
import org.continuity.api.entities.config.ModularizationOptions;
import org.continuity.api.entities.config.Order;
import org.continuity.api.entities.config.OrderGoal;
import org.continuity.api.entities.config.OrderMode;
import org.continuity.api.entities.config.OrderOptions;
import org.continuity.api.entities.config.WorkloadModelType;
import org.continuity.api.entities.links.ForecastLinks;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.links.LoadTestLinks;
import org.continuity.api.entities.links.SessionLogsLinks;
import org.continuity.api.entities.links.SessionsBundlesLinks;
import org.continuity.api.entities.links.WorkloadModelLinks;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.entities.report.OrderResponse;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.commons.utils.WebUtils;
import org.continuity.dsl.description.ForecastInput;
import org.continuity.orchestrator.entities.CreationStep;
import org.continuity.orchestrator.entities.DummyStep;
import org.continuity.orchestrator.entities.OrderReportCounter;
import org.continuity.orchestrator.entities.Recipe;
import org.continuity.orchestrator.entities.RecipeStep;
import org.continuity.orchestrator.orders.OrderCycleManager;
import org.continuity.orchestrator.storage.TestingContextStorage;
import org.continuity.orchestrator.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.client.Channel;

@RestController
@RequestMapping(ROOT)
public class OrchestrationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrchestrationController.class);

	private final MemoryStorage<OrderReportCounter> orderCounterStorage = new MemoryStorage<>(OrderReportCounter.class);

	@Autowired
	private MemoryStorage<Recipe> recipeStorage;

	@Autowired
	@Qualifier("testingContextStorage")
	private TestingContextStorage testingContextStorage;

	private final OrderCycleManager orderCycleManager = new OrderCycleManager();

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Value("${spring.rabbitmq.host}")
	private String rabbitHost;

	@Value("${rabbitmq.management.port:15672}")
	private String rabbitPort;

	@Value("${rabbitmq.management.user:guest}")
	private String rabbitUser;

	@Value("${rabbitmq.management.password:guest}")
	private String rabbitPassword;

	@RequestMapping(path = SUBMIT, method = RequestMethod.POST)
	public ResponseEntity<Object> submitOrder(@RequestBody Order order, HttpServletRequest servletRequest) throws IOException {
		String orderId = orderCounterStorage.reserve(order.getTag());

		LOGGER.info("{} Received new order with goal {} and ID {}.", LoggingUtils.formatPrefix(orderId), order.getGoal().toPrettyString(), orderId);

		boolean useTestingContext = ((order.getTestingContext() != null) && !order.getTestingContext().isEmpty());

		int numRecipes = 1;

		if (useTestingContext && (order.getSource() == null)) {
			Map<Set<String>, Set<LinkExchangeModel>> sources = testingContextStorage.get(order.getTag(), order.getTestingContext(), false);
			if (sources == null) {
				numRecipes = 0;
			} else {
				numRecipes = sources.entrySet().stream().map(entry -> entry.getValue().size()).reduce(Math::addExact).get();
			}

			if (numRecipes == 0) {
				return ResponseEntity.badRequest().body(Collections.singletonMap("error", "No sources found for testing-context " + order.getTestingContext()));
			}

			declareResponseQueue(orderId);
			orderCounterStorage.putToReserved(orderId, new OrderReportCounter(orderId, numRecipes));

			for (Map.Entry<Set<String>, Set<LinkExchangeModel>> entry : sources.entrySet()) {
				for (LinkExchangeModel source : entry.getValue()) {
					createAndSubmitRecipe(orderId, order.getTag(), order.getGoal(), order.getMode(), order.getOptions(),  order.getForecastInput(), entry.getKey(), source, order.getModularizationOptions());
				}
			}
		} else {
			declareResponseQueue(orderId);
			orderCounterStorage.putToReserved(orderId, new OrderReportCounter(orderId, 1));

			createAndSubmitRecipe(orderId, order.getTag(), order.getGoal(), order.getMode(), order.getOptions(),  order.getForecastInput(), order.getTestingContext(), order.getSource(), order.getModularizationOptions());
		}

		OrderResponse response = new OrderResponse();
		String host = servletRequest.getServerName() + ":" + servletRequest.getServerPort();
		response.setResultLink(RestApi.Orchestrator.Orchestration.RESULT.requestUrl(orderId).withHost(host).get());
		response.setWaitLink(RestApi.Orchestrator.Orchestration.WAIT.requestUrl(orderId).withHost(host).get());
		response.setNumReports(numRecipes);

		return ResponseEntity.accepted().body(response);
	}

	private void createAndSubmitRecipe(String orderId, String tag, OrderGoal goal, OrderMode mode, OrderOptions options, ForecastInput forecastInput, Set<String> testingContext, LinkExchangeModel source, ModularizationOptions modularizationOptions) {
		boolean useTestingContext = ((testingContext != null) && !testingContext.isEmpty());

		if (useTestingContext) {
			LOGGER.info("{} Using testing-context {}.", LoggingUtils.formatPrefix(orderId), testingContext);
		}

		if (mode == null) {
			List<OrderMode> modes = orderCycleManager.getModesContainingGoal(goal);

			if (modes.size() == 1) {
				mode = modes.get(0);
			} else {
				LOGGER.error("{} Order is ambiguous! The goal {} is contained in the modes {}.", LoggingUtils.formatPrefix(orderId), goal, modes);

				amqpTemplate.convertAndSend(AmqpApi.Orchestrator.EVENT_FINISHED.name(), AmqpApi.Orchestrator.EVENT_FINISHED.formatRoutingKey().of(orderId),
						OrderReport.asError(orderId, source, "The order goal is ambiguous: there are " + modes.size() + " possible modes " + modes));

				return;
			}
		}

		String recipeId = recipeStorage.reserve(tag);
		List<RecipeStep> recipeSteps = new ArrayList<>();

		for (OrderGoal subGoal : orderCycleManager.getCycle(mode, goal)) {
			recipeSteps.add(createRecipeStep(orderId, recipeId, tag, subGoal, options));
		}

		LOGGER.info("{} Processing new recipe with goal {}...", LoggingUtils.formatPrefix(orderId, recipeId), goal);

		Recipe recipe = new Recipe(orderId, recipeId, tag, recipeSteps, source, useTestingContext, testingContext, options, modularizationOptions, forecastInput);

		if (recipe.hasNext()) {
			recipeStorage.putToReserved(recipeId, recipe);

			recipe.next().execute();
		} else {
			LOGGER.info("{} No tasks required.", LoggingUtils.formatPrefix(orderId, recipeId));

			amqpTemplate.convertAndSend(AmqpApi.Orchestrator.EVENT_FINISHED.name(), AmqpApi.Orchestrator.EVENT_FINISHED.formatRoutingKey().of(orderId),
					OrderReport.asSuccessful(orderId, testingContext, recipe.getSource()));
		}
	}

	@RequestMapping(path = WAIT, method = RequestMethod.GET)
	public ResponseEntity<OrderReport> waitUntilFinished(@PathVariable("id") String orderId, @RequestParam long timeout, HttpServletRequest servletRequest) {
		LOGGER.info("{} Waiting {} ms for the result to be created", LoggingUtils.formatPrefix(orderId), timeout);

		OrderReport report;
		try {
			report = amqpTemplate.receiveAndConvert(getResponseQueueName(orderId), timeout, ParameterizedTypeReference.forType(OrderReport.class));
		} catch (AmqpIOException e) {
			LOGGER.error("{} Cannot wait for not existing response queue of recipe {}", LoggingUtils.formatPrefix(orderId));
			LOGGER.error("Exception:", e);

			return ResponseEntity.badRequest().body(OrderReport.asError(orderId, null, "There is no such order!"));
		}

		if (report != null) {
			OrderReportCounter reportCounter = orderCounterStorage.get(orderId);
			int reportNumber = reportCounter.nextReportNumber();

			if (reportNumber == reportCounter.getNumReports()) {
				deleteResponseQueue(orderId);
				LOGGER.info("{} Returning report number {}/{}. Therefore, deleting the response queue.", LoggingUtils.formatPrefix(orderId), reportNumber, reportCounter.getNumReports());
			}

			report.setCreatedArtifacts(transfromToExternalLinks(report.getInternalArtifacts(), servletRequest.getServerName() + ":" + servletRequest.getServerPort()));
			report.setNumber(reportNumber);
			report.setMax(reportCounter.getNumReports());

			LOGGER.info("{} Report is ready.", LoggingUtils.formatPrefix(orderId));
			return ResponseEntity.ok(report);
		} else {
			LOGGER.info("{} Report is not ready yet.", LoggingUtils.formatPrefix(orderId));

			return ResponseEntity.noContent().build();
		}
	}

	private <S, T> Function<LinkExchangeModel, Boolean> isPresent(Function<LinkExchangeModel, S> getLinksObject, Function<S, T> getLink) {
		return getLinksObject.andThen(getLink).andThen(x -> x != null);
	}

	private <S, T> Function<LinkExchangeModel, Boolean> isEqual(Function<LinkExchangeModel, S> getLinksObject, Function<S, T> getLink, T value) {
		return getLinksObject.andThen(getLink).andThen(value::equals);
	}

	@SafeVarargs
	private final Function<LinkExchangeModel, Boolean> all(Function<LinkExchangeModel, Boolean>... functions) {
		return links -> Arrays.stream(functions).map(f -> f.apply(links)).reduce((a, b) -> a && b).get();
	}

	@RequestMapping(path = RESULT, method = RequestMethod.GET)
	public ResponseEntity<OrderReport> getResultWithoutWaiting(@PathVariable("id") String orderId, HttpServletRequest servletRequest) {
		LOGGER.info("{} Trying to get result without waiting...", LoggingUtils.formatPrefix(orderId));
		return waitUntilFinished(orderId, 0, servletRequest);
	}

	private RecipeStep createRecipeStep(String orderId, String recipeId, String tag, OrderGoal goal, OrderOptions options) {
		RecipeStep step;
		String stepName = goal.toPrettyString();

		switch (goal) {
		case CREATE_SESSION_LOGS:
			step = new CreationStep(stepName, orderId, recipeId, amqpTemplate, AmqpApi.SessionLogs.TASK_CREATE, AmqpApi.SessionLogs.TASK_CREATE.formatRoutingKey().of(tag),
					isPresent(LinkExchangeModel::getSessionLogsLinks, SessionLogsLinks::getLink));
			break;
		case CREATE_BEHAVIOR_MIX:
			WorkloadModelType workloadType;
			if ((options == null) || (options.getWorkloadModelType() == null)) {
				workloadType = WorkloadModelType.WESSBAS;
			} else {
				workloadType = options.getWorkloadModelType();
			}

			step = new CreationStep(stepName, orderId, recipeId, amqpTemplate, AmqpApi.WorkloadModel.MIX_CREATE, AmqpApi.WorkloadModel.MIX_CREATE.formatRoutingKey().of(workloadType.toPrettyString()),
					isPresent(LinkExchangeModel::getSessionsBundlesLinks, SessionsBundlesLinks::getLink));
			break;
		case CREATE_FORECAST:
			step = new CreationStep(stepName, orderId, recipeId, amqpTemplate, AmqpApi.Forecast.TASK_CREATE, AmqpApi.Forecast.TASK_CREATE.formatRoutingKey().of("forecast"),
					isPresent(LinkExchangeModel::getForecastLinks, ForecastLinks::getLink));
			break;
		case CREATE_WORKLOAD_MODEL:
			if ((options == null) || (options.getWorkloadModelType() == null)) {
				workloadType = WorkloadModelType.WESSBAS;
			} else {
				workloadType = options.getWorkloadModelType();
			}

			Function<LinkExchangeModel, Boolean> check = all(isPresent(LinkExchangeModel::getWorkloadModelLinks, WorkloadModelLinks::getLink),
					isEqual(LinkExchangeModel::getWorkloadModelLinks, WorkloadModelLinks::getType, workloadType));

			step = new CreationStep(stepName, orderId, recipeId, amqpTemplate, AmqpApi.WorkloadModel.TASK_CREATE,
					AmqpApi.WorkloadModel.TASK_CREATE.formatRoutingKey().of(workloadType.toPrettyString()), check);
			break;
		case CREATE_LOAD_TEST:
			LoadTestType loadTestType;

			if ((options == null) || (options.getLoadTestType() == null)) {
				loadTestType = LoadTestType.JMETER;
			} else {
				loadTestType = options.getLoadTestType();
			}

			check = all(isPresent(LinkExchangeModel::getLoadTestLinks, LoadTestLinks::getLink),
					isEqual(LinkExchangeModel::getLoadTestLinks, LoadTestLinks::getType, loadTestType));

			step = new CreationStep(stepName, orderId, recipeId, amqpTemplate, AmqpApi.LoadTest.TASK_CREATE, AmqpApi.LoadTest.TASK_CREATE.formatRoutingKey().of(loadTestType.toPrettyString()), check);
			break;
		case EXECUTE_LOAD_TEST:
			loadTestType = options.getLoadTestType();

			if (loadTestType == null) {
				loadTestType = LoadTestType.JMETER;
			}

			if (loadTestType.canExecute()) {
				step = new CreationStep(stepName, orderId, recipeId, amqpTemplate, AmqpApi.LoadTest.TASK_EXECUTE, AmqpApi.LoadTest.TASK_EXECUTE.formatRoutingKey().of(loadTestType.toPrettyString()),
						links -> false);
			} else {
				LOGGER.error("Cannot execute {} tests!", loadTestType);
				step = new DummyStep(amqpTemplate);
			}

			break;
		default:
			LOGGER.error("Cannot create {} step. Unknown goal!", goal);
			step = new DummyStep(amqpTemplate);
			break;

		}

		return step;
	}

	@PostConstruct
	private void clearResponseQueues() {
		RabbitManagementTemplate template = new RabbitManagementTemplate(WebUtils.buildUrl(rabbitHost, rabbitPort, "/api/"), rabbitUser, rabbitPassword);
		String regex = AmqpApi.Orchestrator.EVENT_FINISHED.deriveQueueName("(.+)");

		for (Queue queue : template.getQueues()) {
			Matcher matcher = Pattern.compile(regex).matcher(queue.getName());

			if (matcher.matches()) {
				LOGGER.info("Deleting finished queue for old order {}.", matcher.group(1));

				template.deleteQueue(queue);
			}
		}
	}

	private void declareResponseQueue(String orderId) {
		try {
			Connection connection = connectionFactory.createConnection();
			Channel channel = connection.createChannel(false);

			String queueName = getResponseQueueName(orderId);
			channel.queueDeclare(queueName, false, false, false, Collections.emptyMap());
			channel.queueBind(queueName, AmqpApi.Orchestrator.EVENT_FINISHED.name(), AmqpApi.Orchestrator.EVENT_FINISHED.formatRoutingKey().of(orderId));

			LOGGER.info("{} Declared a response queue.", LoggingUtils.formatPrefix(orderId));
		} catch (IOException e) {
			LOGGER.error("Could not create a response queue!", e);
		}
	}

	private void deleteResponseQueue(String orderId) {
		try {
			Connection connection = connectionFactory.createConnection();
			Channel channel = connection.createChannel(false);

			String queueName = getResponseQueueName(orderId);
			channel.queueDelete(queueName);

			LOGGER.info("{} Deleted the response queue.", LoggingUtils.formatPrefix(orderId));
		} catch (IOException e) {
			LOGGER.error("Could not create a response queue!", e);
		}
	}

	private String getResponseQueueName(String orderId) {
		return AmqpApi.Orchestrator.EVENT_FINISHED.deriveQueueName(orderId);
	}

	private LinkExchangeModel transfromToExternalLinks(LinkExchangeModel internal, String host) {
		LinkExchangeModel external = new LinkExchangeModel();

		if (internal.getSessionLogsLinks().getLink() != null) {
			List<String> params = RestApi.SessionLogs.GET.parsePathParameters(internal.getSessionLogsLinks().getLink());

			if (params != null) {
				external.getSessionLogsLinks().setLink(RestApi.Orchestrator.SessionLogs.GET.requestUrl(params.get(0)).withHost(host).get());
			} else {
				LOGGER.warn("The link {} does not match the endpoint {}!", internal.getSessionLogsLinks().getLink(), RestApi.Orchestrator.SessionLogs.GET.genericPath());
			}
		}

		if (internal.getWorkloadModelLinks().getLink() != null) {
			WorkloadModelType type = internal.getWorkloadModelLinks().getType();

			if (type != null) {
				List<String> params = RestApi.Generic.WORKLOAD_MODEL_LINK.get(type.toPrettyString()).parsePathParameters(internal.getWorkloadModelLinks().getLink());

				if (params != null) {
					external.getWorkloadModelLinks().setType(type);
					external.getWorkloadModelLinks().setLink(RestApi.Orchestrator.WorkloadModel.GET.requestUrl(type.toPrettyString(), params.get(0)).withHost(host).get());
				} else {
					LOGGER.warn("The link {} does not match the endpoint {}!", internal.getWorkloadModelLinks().getLink(), RestApi.Orchestrator.WorkloadModel.GET.genericPath());
				}
			}
		}

		if (internal.getLoadTestLinks().getLink() != null) {
			LoadTestType type = internal.getLoadTestLinks().getType();

			if (type != null) {
				List<String> params = RestApi.Generic.GET_LOAD_TEST.get(type.toPrettyString()).parsePathParameters(internal.getLoadTestLinks().getLink());

				if (params != null) {
					external.getLoadTestLinks().setType(type);
					external.getLoadTestLinks().setLink(RestApi.Orchestrator.Loadtest.GET.requestUrl(type.toPrettyString(), params.get(0)).withHost(host).get());
				} else {
					LOGGER.warn("The link {} does not match the endpoint {}!", internal.getLoadTestLinks().getLink(), RestApi.Orchestrator.Loadtest.GET.genericPath());
				}
			}
		}

		if (internal.getLoadTestLinks().getReportLink() != null) {
			LoadTestType type = internal.getLoadTestLinks().getType();

			if (type != null) {
				List<String> params = RestApi.Generic.GET_LOAD_TEST_REPORT.get(type.toPrettyString()).parsePathParameters(internal.getLoadTestLinks().getReportLink());

				if (params != null) {
					external.getLoadTestLinks().setType(type);
					external.getLoadTestLinks().setReportLink(RestApi.Orchestrator.Loadtest.REPORT.requestUrl(type.toPrettyString(), params.get(0)).withHost(host).get());
				} else {
					LOGGER.warn("The link {} does not match the endpoint {}!", internal.getLoadTestLinks().getReportLink(), RestApi.Orchestrator.Loadtest.REPORT.genericPath());
				}
			}
		}

		return external;
	}

}
