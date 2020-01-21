package org.continuity.orchestrator.controllers;

import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.ROOT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.RESULT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.SUBMIT;
import static org.continuity.api.rest.RestApi.Orchestrator.Orchestration.Paths.WAIT;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.jms.IllegalStateException;
import javax.servlet.http.HttpServletRequest;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.ArtifactType;
import org.continuity.api.entities.order.Order;
import org.continuity.api.entities.order.OrderOptions;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.entities.report.OrderResponse;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.commons.utils.WebUtils;
import org.continuity.orchestrator.entities.CreationStep;
import org.continuity.orchestrator.entities.OrderReportCounter;
import org.continuity.orchestrator.entities.Recipe;
import org.continuity.orchestrator.entities.RecipeStep;
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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
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

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private EurekaClient eurekaClient;

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
		String orderId = orderCounterStorage.reserve(order.getAppId());

		LOGGER.info("{} Received new order with goal {} and ID {}.", LoggingUtils.formatPrefix(orderId), order.getTarget().toPrettyString(), orderId);

		boolean useTestingContext = ((order.getTestingContext() != null) && !order.getTestingContext().isEmpty());

		int numRecipes = 1;

		if (useTestingContext && (order.getSource() == null)) {
			Map<Set<String>, Set<ArtifactExchangeModel>> sources = testingContextStorage.get(order.getAppId(), order.getTestingContext(), false);
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

			for (Map.Entry<Set<String>, Set<ArtifactExchangeModel>> entry : sources.entrySet()) {
				for (ArtifactExchangeModel source : entry.getValue()) {
					try {
						createAndSubmitRecipe(orderId, order, entry.getKey(), source);
					} catch (IllegalStateException e) {
						LOGGER.error("{} Cannot submit order: {}", LoggingUtils.formatPrefix(orderId), e.getMessage());
						return ResponseEntity.badRequest().body(e.getMessage());
					}
				}
			}
		} else {
			declareResponseQueue(orderId);
			orderCounterStorage.putToReserved(orderId, new OrderReportCounter(orderId, 1));

			try {
				createAndSubmitRecipe(orderId, order, order.getTestingContext(), order.getSource());
			} catch (IllegalStateException e) {
				LOGGER.error("{} Cannot submit order: {}", LoggingUtils.formatPrefix(orderId), e.getMessage());
				return ResponseEntity.badRequest().body(e.getMessage());
			}
		}

		OrderResponse response = new OrderResponse();
		String host = servletRequest.getServerName() + ":" + servletRequest.getServerPort();
		response.setResultLink(RestApi.Orchestrator.Orchestration.RESULT.requestUrl(orderId).withHost(host).get());
		response.setWaitLink(RestApi.Orchestrator.Orchestration.WAIT.requestUrl(orderId).withHost(host).get());
		response.setNumReports(numRecipes);

		return ResponseEntity.accepted().body(response);
	}

	public void createAndSubmitRecipe(String orderId, Order order, Set<String> testingContext, ArtifactExchangeModel source) throws IllegalStateException {
		boolean useTestingContext = ((testingContext != null) && !testingContext.isEmpty());

		if (useTestingContext) {
			LOGGER.info("{} Using testing-context {}.", LoggingUtils.formatPrefix(orderId), testingContext);
		}

		if (source == null) {
			source = new ArtifactExchangeModel();
		}

		Map<ArtifactType, Map<String, ArtifactType>> producerMap = createProducerMap();
		OrderOptions options = order.getOptions() == null ? new OrderOptions() : order.getOptions();
		Map<ArtifactType, String> producers = options.getProducersOrDefault();

		String recipeId = recipeStorage.reserve(order.getAppId());
		List<RecipeStep> steps = new LinkedList<>();
		ArtifactType target = order.getTarget();

		while (target != null) {
			String service = producers.get(target);

			if ((producerMap.get(target) == null) || !producerMap.get(target).containsKey(service)) {
				throw new IllegalStateException("There is no " + service + " service available to produce a " + target.toPrettyString() + "!");
			}

			RecipeStep step = new CreationStep(target, orderId, recipeId, amqpTemplate, service, this::isServiceAvailable);
			steps.add(0, step);

			target = producerMap.get(target).get(service);
		}

		LOGGER.info("{} Processing new recipe with target {}...", LoggingUtils.formatPrefix(orderId, recipeId), order.getTarget());

		Recipe recipe = new Recipe(orderId, recipeId, order.getAppId(), order.getServices(), order.getVersion(), order.getPerspective(), steps, source, useTestingContext, testingContext, options,
				order.getWorkloadDescription());

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

			report.setNumber(reportNumber);
			report.setMax(reportCounter.getNumReports());

			LOGGER.info("{} Report is ready.", LoggingUtils.formatPrefix(orderId));
			return ResponseEntity.ok(report);
		} else {
			LOGGER.info("{} Report is not ready yet.", LoggingUtils.formatPrefix(orderId));

			return ResponseEntity.noContent().build();
		}
	}

	@RequestMapping(path = RESULT, method = RequestMethod.GET)
	public ResponseEntity<OrderReport> getResultWithoutWaiting(@PathVariable("id") String orderId, HttpServletRequest servletRequest) {
		LOGGER.info("{} Trying to get result without waiting...", LoggingUtils.formatPrefix(orderId));
		return waitUntilFinished(orderId, 0, servletRequest);
	}

	private boolean isServiceAvailable(String service) {
		return eurekaClient.getApplications().getRegisteredApplications().stream().map(Application::getInstances).flatMap(List::stream).map(InstanceInfo::getAppName).map(String::toLowerCase)
				.collect(Collectors.toSet()).contains(service);
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

	/**
	 * Creates a map {@code target -> producing-service -> required}, e.g., <br>
	 * <br>
	 *
	 * <code>
	 * test-results: <br>
	 * * jmeter: load-test <br>
	 * load-test: <br>
	 * * jmeter: workload-model <br>
	 * * benchflow: workload-model <br>
	 * workload-model: <br>
	 * * wessbas: behavior-model <br>
	 * * request-rates: traces <br>
	 * behavior-model: <br>
	 * * cobra: context <br>
	 * * wessbas: sessions <br>
	 * sessions: <br>
	 * * cobra: context <br>
	 * traces: <br>
	 * * cobra: context
	 * </code>
	 *
	 * @return
	 */
	private Map<ArtifactType, Map<String, ArtifactType>> createProducerMap() {
		return eurekaClient.getApplications().getRegisteredApplications().stream().map(Application::getInstancesAsIsFromEureka).flatMap(List::stream).map(this::createSingleProducerMap)
				.reduce(this::mergeProducerMaps).orElse(Collections.emptyMap());
	}

	private Map<ArtifactType, Map<String, ArtifactType>> createSingleProducerMap(InstanceInfo info) {
		Map<String, String> metadata = info.getMetadata();

		String produced = metadata.get("produces");

		if (produced == null) {
			return Collections.emptyMap();
		}

		Map<ArtifactType, Map<String, ArtifactType>> producers = new HashMap<>();

		Arrays.stream(produced.split("\\,")).map(String::trim).filter(s -> !s.isEmpty()).forEach(type -> {
			ArtifactType keyType = ArtifactType.fromPrettyString(type);
			ArtifactType valueType = Optional.ofNullable(metadata.get("requires-for-" + type)).map(ArtifactType::fromPrettyString).orElse(null);

			Map<String, ArtifactType> inner = new HashMap<>();
			inner.put(info.getAppName().toLowerCase(), valueType);
			producers.put(keyType, inner);
		});

		return producers;
	}

	private Map<ArtifactType, Map<String, ArtifactType>> mergeProducerMaps(Map<ArtifactType, Map<String, ArtifactType>> first, Map<ArtifactType, Map<String, ArtifactType>> second) {
		if (first.isEmpty()) {
			return second;
		} else if (second.isEmpty()) {
			return first;
		}

		for (Entry<ArtifactType, Map<String, ArtifactType>> entry : second.entrySet()) {
			if (first.containsKey(entry.getKey())) {
				first.get(entry.getKey()).putAll(entry.getValue());
			} else {
				first.put(entry.getKey(), entry.getValue());
			}
		}

		return first;
	}

}
