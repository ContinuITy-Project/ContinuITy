package org.continuity.orchestrator.controllers;

import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.ROOT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.RESULT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.SUBMIT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.WAIT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.LoadTestType;
import org.continuity.api.entities.config.Order;
import org.continuity.api.entities.config.OrderGoal;
import org.continuity.api.entities.config.WorkloadModelType;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.links.LoadTestLinks;
import org.continuity.api.entities.links.SessionLogsLinks;
import org.continuity.api.entities.links.WorkloadModelLinks;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.orchestrator.entities.CreationStep;
import org.continuity.orchestrator.entities.DummyStep;
import org.continuity.orchestrator.entities.Recipe;
import org.continuity.orchestrator.entities.RecipeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.rabbitmq.client.Channel;

@RestController
@RequestMapping(ROOT)
public class OrchestrationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrchestrationController.class);

	@Autowired
	private MemoryStorage<Recipe> storage;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private ConnectionFactory connectionFactory;

	@RequestMapping(path = SUBMIT, method = RequestMethod.POST)
	public ResponseEntity<Map<String, String>> submitOrder(@RequestBody Order order, HttpServletRequest servletRequest) {
		Optional<OrderGoal> goal = Optional.ofNullable(order.getGoal());
		List<RecipeStep> recipeSteps = new ArrayList<>();

		while (goal.isPresent()) {
			recipeSteps.add(createRecipeStep(goal.get(), order));
			goal = goal.get().getRequired();
		}

		recipeSteps = Lists.reverse(recipeSteps);

		String recipeId = storage.reserve(order.getTag());
		LOGGER.info("Processing new recipe {} with goal {}...", recipeId, order.getGoal());

		Recipe recipe = new Recipe(recipeId, recipeSteps, order);

		Map<String, String> responseMap = new HashMap<>();

		if (recipe.hasNext()) {
			declareResponseQueue(recipeId);

			storage.putToReserved(recipeId, recipe);
			recipe.next().execute();

			String host = servletRequest.getServerName() + ":" + servletRequest.getServerPort();
			responseMap.put("result-link", RestApi.Orchestrator.Orchestration.RESULT.requestUrl(recipeId).withHost(host).get());
			responseMap.put("wait-link", RestApi.Orchestrator.Orchestration.WAIT.requestUrl(recipeId).withHost(host).get());

			return ResponseEntity.accepted().body(responseMap);
		} else {
			LOGGER.warn("Created empty recipe {}!", recipeId);
			responseMap.put("error", "No task contained in the order");
			return ResponseEntity.badRequest().body(responseMap);
		}
	}

	@RequestMapping(path = WAIT, method = RequestMethod.GET)
	public ResponseEntity<OrderReport> waitUntilFinished(@PathVariable("id") String orderId, @RequestParam long timeout, HttpServletRequest servletRequest) {
		LOGGER.info("Waiting {} ms for the result of order {} to be created", timeout, orderId);

		OrderReport report;
		try {
			report = amqpTemplate.receiveAndConvert(getResponseQueueName(orderId), timeout, ParameterizedTypeReference.forType(OrderReport.class));
		} catch (AmqpIOException e) {
			LOGGER.error("Cannot wait for not existing response queue of recipe {}", orderId);
			LOGGER.error("Exception:", e);

			return ResponseEntity.badRequest().body(OrderReport.asError(orderId, null, "There is no such order!"));
		}

		if (report != null) {
			deleteResponseQueue(orderId);

			report.setCreatedArtifacts(transfromToExternalLinks(report.getInternalArtifacts(), servletRequest.getServerName() + ":" + servletRequest.getServerPort()));

			LOGGER.info("Report for order {} is ready.", orderId);
			return ResponseEntity.ok(report);
		} else {
			LOGGER.info("Report for order {} is not ready, yet.", orderId);

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
		LOGGER.info("Trying to get result for order {} without waiting...", orderId);
		return waitUntilFinished(orderId, 0, servletRequest);
	}

	private RecipeStep createRecipeStep(OrderGoal goal, Order order) {
		RecipeStep step;
		String stepName = goal.toPrettyString();

		switch (goal) {
		case CREATE_SESSION_LOGS:
			step = new CreationStep(stepName, amqpTemplate, AmqpApi.SessionLogs.TASK_CREATE, AmqpApi.SessionLogs.TASK_CREATE.formatRoutingKey().of(order.getTag()),
					isPresent(LinkExchangeModel::getSessionLogsLinks, SessionLogsLinks::getLink));
			break;
		case CREATE_WORKLOAD_MODEL:
			WorkloadModelType workloadType;
			if ((order.getOptions() == null) || (order.getOptions().getWorkloadModelType() == null)) {
				workloadType = WorkloadModelType.WESSBAS;
			} else {
				workloadType = order.getOptions().getWorkloadModelType();
			}

			Function<LinkExchangeModel, Boolean> check = all(isPresent(LinkExchangeModel::getWorkloadModelLinks, WorkloadModelLinks::getLink),
					isEqual(LinkExchangeModel::getWorkloadModelLinks, WorkloadModelLinks::getType, workloadType));

			step = new CreationStep(stepName, amqpTemplate, AmqpApi.WorkloadModel.TASK_CREATE, AmqpApi.WorkloadModel.TASK_CREATE.formatRoutingKey().of(workloadType.toPrettyString()), check);
			break;
		case CREATE_LOAD_TEST:
			LoadTestType loadTestType;

			if ((order.getOptions() == null) || (order.getOptions().getLoadTestType() == null)) {
				loadTestType = LoadTestType.JMETER;
			} else {
				loadTestType = order.getOptions().getLoadTestType();
			}

			check = all(isPresent(LinkExchangeModel::getLoadTestLinks, LoadTestLinks::getLink),
					isEqual(LinkExchangeModel::getLoadTestLinks, LoadTestLinks::getType, loadTestType));

			step = new CreationStep(stepName, amqpTemplate, AmqpApi.LoadTest.TASK_CREATE, AmqpApi.LoadTest.TASK_CREATE.formatRoutingKey().of(loadTestType.toPrettyString()), check);
			break;
		case EXECUTE_LOAD_TEST:
			loadTestType = order.getOptions().getLoadTestType();

			if (loadTestType == null) {
				loadTestType = LoadTestType.JMETER;
			}

			if (loadTestType.canExecute()) {
				step = new CreationStep(stepName, amqpTemplate, AmqpApi.LoadTest.TASK_EXECUTE, AmqpApi.LoadTest.TASK_EXECUTE.formatRoutingKey().of(loadTestType.toPrettyString()), links -> false);
			} else {
				LOGGER.error("Cannot execute {} tests!", loadTestType);
				step = new DummyStep(amqpTemplate);
			}

			break;
		default:
			LOGGER.error("Cannot create {} step. Unknown goal!", order.getGoal());
			step = new DummyStep(amqpTemplate);
			break;

		}

		return step;
	}

	private void declareResponseQueue(String recipeId) {
		try {
			Connection connection = connectionFactory.createConnection();
			Channel channel = connection.createChannel(false);

			String queueName = getResponseQueueName(recipeId);
			channel.queueDeclare(queueName, false, false, false, Collections.emptyMap());
			channel.queueBind(queueName, AmqpApi.Orchestrator.EVENT_FINISHED.name(), AmqpApi.Orchestrator.EVENT_FINISHED.formatRoutingKey().of(recipeId));

			LOGGER.info("Declared a response queue for {}.", recipeId);
		} catch (IOException e) {
			LOGGER.error("Could not create a response queue!", e);
		}
	}

	private void deleteResponseQueue(String recipeId) {
		try {
			Connection connection = connectionFactory.createConnection();
			Channel channel = connection.createChannel(false);

			String queueName = getResponseQueueName(recipeId);
			channel.queueDelete(queueName);

			LOGGER.info("Deleted the response queue for {}.", recipeId);
		} catch (IOException e) {
			LOGGER.error("Could not create a response queue!", e);
		}
	}

	private String getResponseQueueName(String recipeId) {
		return AmqpApi.Orchestrator.EVENT_FINISHED.deriveQueueName(recipeId);
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
