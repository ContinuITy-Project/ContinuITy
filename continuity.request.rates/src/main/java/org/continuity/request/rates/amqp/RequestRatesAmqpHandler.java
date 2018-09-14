package org.continuity.request.rates.amqp;

import java.util.List;
import java.util.stream.Collectors;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.ExternalDataLinkType;
import org.continuity.api.entities.links.MeasurementDataLinks;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.commons.storage.CsvFileStorage;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.application.Application;
import org.continuity.request.rates.config.RabbitMqConfig;
import org.continuity.request.rates.entities.CsvRow;
import org.continuity.request.rates.entities.RequestRecord;
import org.continuity.request.rates.entities.WorkloadModelPack;
import org.continuity.request.rates.model.RequestRatesModel;
import org.continuity.request.rates.transform.RequestRatesCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class RequestRatesAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestRatesAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MixedStorage<RequestRatesModel> storage;

	@Autowired
	private CsvFileStorage<CsvRow> requestLogsStorage;

	@Value("${spring.application.name}")
	private String applicationName;

	/**
	 * Listener to the RabbitMQ {@link RabbitMqConfig#TASK_CREATE_QUEUE_NAME}. Creates a new request
	 * rates model based on the specified request logs.
	 *
	 * @param task
	 *            The description of the task to be done.
	 */
	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void onMonitoringDataAvailable(TaskDescription task) {
		LOGGER.info("Task {}: Received new task to be processed for tag '{}'", task.getTaskId(), task.getTag());

		TaskReport report;
		MeasurementDataLinks link = task.getSource().getMeasurementDataLinks();

		if (link.getLinkType() != ExternalDataLinkType.CSV) {
			LOGGER.error("Task {}: Cannot process measurement data of type {}!", task.getTaskId(), link.getLinkType());
			report = TaskReport.error(task.getTaskId(), TaskError.ILLEGAL_TYPE);
		} else {
			List<CsvRow> csvRecords;

			if (link.getLink().startsWith(applicationName)) {
				List<String> pathParams = RestApi.RequestRates.RequestLogs.GET.parsePathParameters(link.getLink());
				csvRecords = requestLogsStorage.get(pathParams.get(0));
			} else {
				String csvString = restTemplate.getForObject(WebUtils.addProtocolIfMissing(link.getLink()), String.class);
				csvRecords = CsvRow.listFromString(csvString);
			}

			List<RequestRecord> records = csvRecords.stream().map(CsvRow::toRecord).collect(Collectors.toList());

			Application application;
			try {
				application = restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(task.getTag()).get(), Application.class);
			} catch (HttpStatusCodeException e) {
				LOGGER.info("Could not get application model for tag {}. Response: {} - {}.", task.getTag(), e.getRawStatusCode(), e.getStatusCode().getReasonPhrase());
				application = null;
			}

			RequestRatesCalculator calculator;

			if (application != null) {
				calculator = new RequestRatesCalculator(application);
			} else {
				calculator = new RequestRatesCalculator();
			}

			RequestRatesModel model = calculator.calculate(records);
			String storageId = storage.put(model, task.getTag(), task.isLongTermUse());

			LOGGER.info("Task {}: Created a new request rates model with id '{}'.", task.getTaskId(), storageId);

			WorkloadModelPack responsePack = new WorkloadModelPack(applicationName, storageId, task.getTag());
			report = TaskReport.successful(task.getTaskId(), responsePack);

			if (application == null) {
				amqpTemplate.convertAndSend(AmqpApi.WorkloadModel.EVENT_CREATED.name(), AmqpApi.WorkloadModel.EVENT_CREATED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), responsePack);
			}
		}

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

}
