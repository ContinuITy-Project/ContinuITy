package org.continuity.request.rates.amqp;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.config.ModularizationApproach;
import org.continuity.api.entities.config.ModularizationOptions;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.MeasurementDataLinkType;
import org.continuity.api.entities.links.MeasurementDataLinks;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.commons.openxtrace.OpenXtraceTracer;
import org.continuity.commons.storage.CsvFileStorage;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.commons.utils.ModularizationUtils;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.application.Application;
import org.continuity.request.rates.config.RabbitMqConfig;
import org.continuity.request.rates.entities.CsvRow;
import org.continuity.request.rates.entities.RequestRecord;
import org.continuity.request.rates.entities.WorkloadModelPack;
import org.continuity.request.rates.model.RequestRatesModel;
import org.continuity.request.rates.transform.ModularizingRequestRatesCalculator;
import org.continuity.request.rates.transform.RequestRatesCalculator;
import org.continuity.request.rates.transform.SimpleRequestRatesCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.SubTrace;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import open.xtrace.OPENxtraceUtils;

@Component
public class RequestRatesAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestRatesAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier("plainRestTemplate")
	private RestTemplate plainRestTemplate;

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

		if (link.getLinkType() == MeasurementDataLinkType.CSV) {
			LOGGER.info("Task {}: Processing CSV data...", task.getTaskId());

			List<CsvRow> csvRecords;

			if (link.getLink().startsWith(applicationName)) {
				List<String> pathParams = RestApi.RequestRates.RequestLogs.GET.parsePathParameters(link.getLink());
				csvRecords = requestLogsStorage.get(pathParams.get(0));
			} else {
				String csvString = restTemplate.getForObject(WebUtils.addProtocolIfMissing(link.getLink()), String.class);
				csvRecords = CsvRow.listFromString(csvString);
			}

			List<RequestRecord> records = csvRecords.stream().map(CsvRow::toRecord).collect(Collectors.toList());

			report = processRequests(records, task, false, null);
		} else if (link.getLinkType() == MeasurementDataLinkType.OPEN_XTRACE) {
			LOGGER.info("Task {}: Processing OPEN.xtrace data...", task.getTaskId());

			Iterable<Trace> traces = OPENxtraceUtils.getOPENxtraces(task.getSource(), plainRestTemplate);
			LOGGER.info("Task {}: Retrieved OPEN.xtrace data.", task.getTaskId());

			boolean applyModularization = false;

			if (null != task.getModularizationOptions()) {
				ModularizationOptions modularizationOptions = task.getModularizationOptions();
				applyModularization = modularizationOptions.getModularizationApproach().equals(ModularizationApproach.REQUESTS);
			}

			List<HTTPRequestProcessingImpl> requestsOfInterest;

			if (applyModularization) {
				Collection<String> targetHostNames = ModularizationUtils.getTargetHostNames(task.getModularizationOptions().getServices(), restTemplate);

				requestsOfInterest = StreamSupport.stream(traces.spliterator(), false).map(Trace::getRoot).map(SubTrace::getRoot).map(root -> OpenXtraceTracer.forRootAndHosts(root, targetHostNames))
						.map(OpenXtraceTracer::extractSubtraces).flatMap(List::stream).collect(Collectors.toList());
			} else {
				requestsOfInterest = StreamSupport.stream(traces.spliterator(), false).map(Trace::getRoot).map(SubTrace::getRoot).map(OpenXtraceTracer::forRoot).map(OpenXtraceTracer::extractSubtraces)
						.flatMap(List::stream).collect(Collectors.toList());
			}

			List<RequestRecord> records = requestsOfInterest.stream().map(this::traceToRequestRecord).collect(Collectors.toList());
			report = processRequests(records, task, applyModularization, task.getModularizationOptions());
		} else {
			LOGGER.error("Task {}: Cannot process measurement data of type {}!", task.getTaskId(), link.getLinkType());
			report = TaskReport.error(task.getTaskId(), TaskError.ILLEGAL_TYPE);
		}

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

	private TaskReport processRequests(List<RequestRecord> records, TaskDescription task, boolean modularize, ModularizationOptions modularizationOptions) {
		RequestRatesCalculator calculator;

		if (modularize) {
			calculator = new ModularizingRequestRatesCalculator(ModularizationUtils.getServiceApplicationModels(modularizationOptions.getServices(), restTemplate));
		} else {
			Application application;
			try {
				application = restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(task.getTag()).get(), Application.class);
			} catch (HttpStatusCodeException e) {
				LOGGER.info("Could not get application model for tag {}. Response: {} - {}.", task.getTag(), e.getRawStatusCode(), e.getStatusCode().getReasonPhrase());
				application = null;
			}

			calculator = new SimpleRequestRatesCalculator(application);
		}

		RequestRatesModel model = calculator.calculate(records);
		String storageId = storage.put(model, task.getTag(), task.isLongTermUse());

		LOGGER.info("Task {}: Created a new request rates model with id '{}'.", task.getTaskId(), storageId);

		WorkloadModelPack responsePack = new WorkloadModelPack(applicationName, storageId, task.getTag());
		TaskReport report = TaskReport.successful(task.getTaskId(), responsePack);

		if (calculator.useNames()) {
			amqpTemplate.convertAndSend(AmqpApi.WorkloadModel.EVENT_CREATED.name(), AmqpApi.WorkloadModel.EVENT_CREATED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), responsePack);
		}

		return report;
	}

	private RequestRecord traceToRequestRecord(HTTPRequestProcessingImpl trace) {
		RequestRecord record = new RequestRecord();

		record.setDomain(trace.getContainingSubTrace().getLocation().getHost());
		record.setPort(Integer.toString(trace.getContainingSubTrace().getLocation().getPort()));
		record.setPath(trace.getUri());
		record.setProtocol("HTTP");

		if (trace.getRequestMethod().isPresent()) {
			record.setMethod(trace.getRequestMethod().get().name());
		}

		record.setEncoding("<no-encoding>");

		if (trace.getContainingSubTrace().getLocation().getBusinessTransaction().isPresent()) {
			record.setName(trace.getContainingSubTrace().getLocation().getBusinessTransaction().get());
		}

		if (trace.getHTTPHeaders().isPresent()) {
			record.setHeaders(trace.getHTTPHeaders().get().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.toList()));
		}

		if (trace.getHTTPParameters().isPresent()) {
			record.setParameters(trace.getHTTPParameters().get().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList()));
		}

		// The timestamp unit is milliseconds
		record.setStartDate(new Date(trace.getTimestamp()));

		// The response time unit is nanoseconds
		record.setEndDate(new Date(trace.getTimestamp() + (trace.getResponseTime() / 1000000)));

		return record;
	}

}
