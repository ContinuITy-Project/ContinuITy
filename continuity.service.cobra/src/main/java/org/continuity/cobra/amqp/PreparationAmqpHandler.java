package org.continuity.cobra.amqp;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.entities.order.TailoringApproach;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RequestBuilder;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestEndpoint;
import org.continuity.cobra.config.RabbitMqConfig;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.cobra.managers.ElasticsearchTraceManager;
import org.continuity.commons.utils.TailoringUtils;
import org.continuity.dsl.WorkloadDescription;
import org.continuity.dsl.elements.timeframe.Timerange;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PreparationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreparationAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private ElasticsearchSessionManager elasticSessionManager;

	@Autowired
	private ElasticsearchTraceManager elasticTraceManager;

	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void prepareInitialData(TaskDescription task) throws IOException, TimeoutException {
		TaskReport report;

		Date from = null;
		Date to = null;
		WorkloadDescription description = task.getWorkloadDescription();

		if ((description != null) && (description.getTimeframe() != null)) {
			from = description.getTimeframe().stream().filter(Timerange.class::isInstance).map(Timerange.class::cast).map(Timerange::getFrom).filter(Optional::isPresent).map(Optional::get)
					.map(d -> d.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).reduce(Math::min).map(Date::new).orElse(null);

			to = description.getTimeframe().stream().filter(Timerange.class::isInstance).map(Timerange.class::cast).map(Timerange::getTo).filter(Optional::isPresent).map(Optional::get)
					.map(d -> d.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).reduce(Math::max).map(Date::new).orElse(null);

			LOGGER.warn("Currently, only the timerange from and to when specifications are used!");
		}

		// TODO: read intensities based on context and use the resulting time ranges for retrieving
		// the corresponding artifacts (store the link in a storage?)

		LOGGER.info("Processing task {}: Get {} from {} to {}...", task.getTaskId(), task.getTarget().toPrettyString(), from, to);

		switch (task.getTarget()) {
		case TRACES:
			report = createTraceLink(task, from, to);
			break;
		case SESSIONS:
			report = createSessionLink(task, from, to);
			break;
		case BEHAVIOR_MODEL:
			LOGGER.error("Task {}: Using the behavior models is not implemented yet!!", task.getTaskId());
			report = TaskReport.error(task.getTaskId(), TaskError.ILLEGAL_TYPE);
			break;
		default:
			LOGGER.error("Task {}: Cannot generate {}!", task.getTaskId(), task.getTarget().toPrettyString());
			report = TaskReport.error(task.getTaskId(), TaskError.ILLEGAL_TYPE);
			break;
		}

		sendReport(report);
	}

	private TaskReport createTraceLink(TaskDescription task, Date from, Date to) throws IOException {
		long count = elasticTraceManager.countTraces(task.getAppId(), null, from, to);

		if (count == 0) {
			LOGGER.error("Task {}: There are no such traces available!", task.getTaskId());
			return TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			ArtifactExchangeModel artifacts = new ArtifactExchangeModel();
			artifacts.getTraceLinks().setLink(formatTraceLink(task.getAppId(), task.getVersion(), from, to));
			return TaskReport.successful(task.getTaskId(), artifacts);
		}
	}

	private TaskReport createSessionLink(TaskDescription task, Date from, Date to) throws IOException {
		List<String> services = extractServices(task);

		long count = elasticSessionManager.countSessionsInRange(task.getAppId(), null, services, from, to);

		if (count == 0) {
			LOGGER.error("Task {}: There are no such sessions available!", task.getTaskId());
			return TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			ArtifactExchangeModel artifacts = new ArtifactExchangeModel();
			artifacts.getSessionLinks().setSimpleLink(formatSessionLink(RestApi.Cobra.Sessions.GET_SIMPLE, task.getAppId(), services, from, to))
					.setExtendedLink(formatSessionLink(RestApi.Cobra.Sessions.GET_EXTENDED, task.getAppId(), services, from, to));
			return TaskReport.successful(task.getTaskId(), artifacts);
		}
	}

	private String formatSessionLink(RestEndpoint endpoint, AppId aid, List<String> services, Date from, Date to) {
		return endpoint.requestUrl(aid.dropService(), Session.convertTailoringToString(services)).withQueryIfNotEmpty("from", ApiFormats.formatOrNull(from))
				.withQueryIfNotEmpty("to", ApiFormats.formatOrNull(to)).withoutProtocol().get();
	}

	private String formatTraceLink(AppId aid, VersionOrTimestamp version, Date from, Date to) {
		RequestBuilder reqBuilder;

		if (version == null) {
			reqBuilder = RestApi.Cobra.MeasurementData.GET.requestUrl(aid);
		} else {
			reqBuilder = RestApi.Cobra.MeasurementData.GET_VERSION.requestUrl(aid, version);
		}

		return reqBuilder.withQueryIfNotEmpty("from", ApiFormats.formatOrNull(from)).withQueryIfNotEmpty("to", ApiFormats.formatOrNull(to)).withoutProtocol().get();
	}

	private void sendReport(TaskReport report) {
		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);

		if (report.isSuccessful()) {
			LOGGER.info("Finished task {} successfully.", report.getTaskId());
		} else {
			LOGGER.warn("Finished task {} with errors.", report.getTaskId());
		}
	}

	private List<String> extractServices(TaskDescription task) {
		List<ServiceSpecification> services = task.getEffectiveServices();

		if ((task.getOptions() != null) && (task.getOptions().getTailoringApproachOrDefault() == TailoringApproach.LOG_BASED) && TailoringUtils.doTailoring(services)) {
			return services.stream().map(ServiceSpecification::getService).collect(Collectors.toList());
		} else {
			return Collections.singletonList(AppId.SERVICE_ALL);
		}
	}

}
