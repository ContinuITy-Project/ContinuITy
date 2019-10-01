package org.continuity.cobra.controllers;

import static org.continuity.api.rest.RestApi.Cobra.KnnDIstance.ROOT;
import static org.continuity.api.rest.RestApi.Cobra.KnnDIstance.Paths.CREATE_PLOT;
import static org.continuity.api.rest.RestApi.Cobra.KnnDIstance.Paths.GET_PLOT;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.amqp.ExchangeDefinition;
import org.continuity.api.amqp.RoutingKeyFormatter;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.artifact.session.SessionRequest;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.api.rest.RestApi;
import org.continuity.cobra.entities.ClustinatorInput;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.client.Channel;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 *
 * @author Henning Schulz
 *
 */
@RestController()
@RequestMapping(ROOT)
public class KnnDistanceController {

	private static final Logger LOGGER = LoggerFactory.getLogger(KnnDistanceController.class);

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private ElasticsearchSessionManager sessionManager;

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

	/**
	 * Triggers the creation of a knn distance plot. The plot can later be accessed via the returned
	 * link.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The tailoring.
	 * @return The link to the plot.
	 * @throws TimeoutException
	 * @throws IOException
	 */
	@RequestMapping(path = CREATE_PLOT, method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "tailoring", required = true, dataType = "string", paramType = "path") })
	public String createPlot(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable String tailoring) throws IOException, TimeoutException {
		CobraConfiguration config = configProvider.getConfiguration(aid);

		List<String> tailoringList = Session.convertStringToTailoring(tailoring);

		Date earliest = sessionManager.getEarliestDate(aid, null, tailoringList);
		Date latest = sessionManager.getLatestDate(aid, null, tailoringList);

		long timeout = config.getSessions().getTimeout().toMillis();
		long interval = config.getClustering().getInterval().toMillis();
		long overlap = config.getClustering().getOverlap().toMillis();

		long to = clusteringTimestamp(latest, interval, timeout);
		long from = to - interval - overlap;

		if (from < earliest.getTime()) {
			long diff = earliest.getTime() - from;
			from = earliest.getTime();
			to = Math.min(to + diff, latest.getTime());
		}

		Date dFrom = new Date(from);
		Date dTo = new Date(to);

		LOGGER.info("Creating knn distance plot for {}.{} in range {} - {}...", aid, tailoringList, dFrom, dTo);

		List<Session> sessions = sessionManager.readSessionsInRange(aid, null, tailoringList, dFrom, dTo);

		List<String> endpoints = sessions.stream().map(Session::getRequests).flatMap(Set::stream).map(SessionRequest::getEndpoint).distinct().collect(Collectors.toList());
		endpoints.add(0, RelativeMarkovChain.INITIAL_STATE);
		endpoints.add(RelativeMarkovChain.FINAL_STATE);

		ClustinatorInput input = new ClustinatorInput();
		input.setAppId(aid);
		input.setTailoring(tailoringList);
		input.setMinSampleSize(config.getClustering().getMinSampleSize());
		input.setSessions(sessions);
		input.setStates(endpoints);

		declareResponseQueue(aid, tailoring);

		ExchangeDefinition<RoutingKeyFormatter.AppId> exchange = AmqpApi.Cobra.Clustinator.TASK_KNN_DISTANCE;
		amqpTemplate.convertAndSend(exchange.name(), exchange.formatRoutingKey().of(aid), input);

		String link = RestApi.Cobra.KnnDIstance.GET_PLOT.requestUrl(aid, tailoring).withoutProtocol().get();

		LOGGER.info("Knn distance plot creation for {}.{} triggered. Will be accessible at {}", aid, tailoringList, link);

		return link;
	}

	private long clusteringTimestamp(Date date, long intervalMillis, long timeoutMillis) {
		return ((date.getTime() - timeoutMillis) / intervalMillis) * intervalMillis;
	}

	/**
	 * Gets the plotted image if ready / existing or returns 404 otherwise.
	 *
	 * @param aid
	 *            The app-id.
	 * @param tailoring
	 *            The tailoring.
	 * @param timeout
	 *            The time to wait until an image is ready in milliseconds.
	 * @return The plotted image as byte array.
	 */
	@RequestMapping(path = GET_PLOT, method = RequestMethod.GET, produces = "application/pdf")
	@ApiImplicitParams({ @ApiImplicitParam(name = "app-id", required = true, dataType = "string", paramType = "path"),
			@ApiImplicitParam(name = "tailoring", required = true, dataType = "string", paramType = "path") })
	public ResponseEntity<byte[]> getPlotImage(@ApiIgnore @PathVariable("app-id") AppId aid, @ApiIgnore @PathVariable String tailoring, long timeout) {
		String responseQueue = getResponseQueueName(aid, tailoring);

		RabbitManagementTemplate template = new RabbitManagementTemplate(WebUtils.buildUrl(rabbitHost, rabbitPort, "/api/"), rabbitUser, rabbitPassword);
		boolean queueExists = template.getQueues().stream().map(Queue::getName).map(n -> n.equals(responseQueue)).reduce(Boolean::logicalOr).orElse(false);

		if (!queueExists) {
			return ResponseEntity.notFound().build();
		}

		Message message = amqpTemplate.receive(responseQueue, timeout);

		if (message == null) {
			return ResponseEntity.notFound().build();
		}

		LOGGER.info("Returning knn distance plot for {}.[{}].", aid, tailoring);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_PNG);

		return ResponseEntity.ok().headers(headers).body(message.getBody());
	}

	@PostConstruct
	private void clearResponseQueues() {
		RabbitManagementTemplate template = new RabbitManagementTemplate(WebUtils.buildUrl(rabbitHost, rabbitPort, "/api/"), rabbitUser, rabbitPassword);
		String regex = AmqpApi.Cobra.Clustinator.EVENT_IMAGEGENERATED.deriveQueueName("(.+)");

		for (Queue queue : template.getQueues()) {
			Matcher matcher = Pattern.compile(regex).matcher(queue.getName());

			if (matcher.matches()) {
				LOGGER.info("Deleting finished queue for old order {}.", matcher.group(1));

				template.deleteQueue(queue);
			}
		}
	}

	private void declareResponseQueue(AppId aid, String tailoring) {
		try {
			Connection connection = connectionFactory.createConnection();
			Channel channel = connection.createChannel(false);

			String queueName = getResponseQueueName(aid, tailoring);
			channel.queueDeclare(queueName, false, false, false, Collections.emptyMap());
			channel.queueBind(queueName, AmqpApi.Cobra.Clustinator.EVENT_IMAGEGENERATED.name(), AmqpApi.Cobra.Clustinator.EVENT_IMAGEGENERATED.formatRoutingKey().of(aid));

			LOGGER.info("Declared a response queue for {}.{}.", aid, tailoring);
		} catch (IOException e) {
			LOGGER.error("Could not create a response queue!", e);
		}
	}

	private String getResponseQueueName(AppId aid, String tailoring) {
		return AmqpApi.Cobra.Clustinator.EVENT_IMAGEGENERATED.deriveQueueName(new StringBuilder().append(aid).append(".").append(tailoring).toString());
	}

}
