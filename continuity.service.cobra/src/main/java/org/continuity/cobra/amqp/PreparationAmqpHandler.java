package org.continuity.cobra.amqp;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.entities.order.TailoringApproach;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RequestBuilder;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestEndpoint;
import org.continuity.cobra.config.RabbitMqConfig;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.commons.utils.TailoringUtils;
import org.continuity.dsl.context.Context;
import org.continuity.dsl.context.timespec.AfterSpecification;
import org.continuity.dsl.context.timespec.BeforeSpecification;
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
	private ElasticsearchSessionManager elasticManager;

	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void createSessionLogs(TaskDescription task) throws IOException, TimeoutException {
		TaskReport report;
		AppId aid = task.getAppId();

		Date from = null;
		Date to = null;
		Context context = task.getContext();

		if ((context != null) && (context.getWhen() != null)) {
			from = context.getWhen().stream().filter(AfterSpecification.class::isInstance).map(AfterSpecification.class::cast).map(AfterSpecification::getDate).map(Date::getTime).reduce(Math::max)
					.map(Date::new).orElse(null);
			to = context.getWhen().stream().filter(BeforeSpecification.class::isInstance).map(BeforeSpecification.class::cast).map(BeforeSpecification::getDate).map(Date::getTime).reduce(Math::min)
					.map(Date::new).orElse(null);

			LOGGER.warn("Currently, only the before and after when specifications are used!");
		}

		LOGGER.info("Processing task {}: Get sessions from {} to {}...", task.getTaskId(), from, to);

		List<String> services = extractServices(task);

		long count = elasticManager.countSessionsInRange(aid, null, services, from, to);

		if (count == 0) {
			LOGGER.error("Task {}: There are no such sessions available!", task.getTaskId());
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			LinkExchangeModel artifacts = new LinkExchangeModel();

			artifacts.getSessionLogsLinks().setSimpleLink(formatSessionLink(RestApi.Cobra.Sessions.GET_SIMPLE, aid, services, from, to))
					.setExtendedLink(formatSessionLink(RestApi.Cobra.Sessions.GET_EXTENDED, aid, services, from, to));

			artifacts.getTraceLinks().setLink(formatTraceLink(aid, task.getVersion(), from, to));

			report = TaskReport.successful(task.getTaskId(), artifacts);
		}

		sendReport(report);
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
