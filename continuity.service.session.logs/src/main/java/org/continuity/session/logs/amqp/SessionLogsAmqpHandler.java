package org.continuity.session.logs.amqp;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.config.ModularizationOptions;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.links.TraceLinks;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestEndpoint;
import org.continuity.idpa.AppId;
import org.continuity.session.logs.config.RabbitMqConfig;
import org.continuity.session.logs.managers.ElasticsearchSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SessionLogsAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private ElasticsearchSessionManager elasticManager;

	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void createSessionLogs(TaskDescription task) throws IOException, TimeoutException {
		TaskReport report;
		AppId aid = task.getAppId();
		TraceLinks links = task.getSource().getTraceLinks();

		LOGGER.info("Processing task {}: Get sessions from {} to {}...", task.getTaskId(), links.getFrom(), links.getTo());

		List<String> services;

		if (null != task.getModularizationOptions()) {
			ModularizationOptions modularizationOptions = task.getModularizationOptions();
			services = modularizationOptions.getServices().keySet().stream().map(AppId::getService).collect(Collectors.toList());
		} else {
			services = Collections.singletonList(AppId.SERVICE_ALL);
		}

		long count = elasticManager.countSessionsInRange(aid, null, services, links.getFrom(), links.getTo());

		if (count == 0) {
			LOGGER.error("Task {}: There are no such sessions available!", task.getTaskId());
			report = TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			report = TaskReport.successful(task.getTaskId(), new LinkExchangeModel().getSessionLogsLinks().setSimpleLink(formatLink(RestApi.SessionLogs.Sessions.GET_SIMPLE, aid, services, links))
					.setExtendedLink(formatLink(RestApi.SessionLogs.Sessions.GET_EXTENDED, aid, services, links)).parent());
		}

		sendReport(report);
	}

	private String formatLink(RestEndpoint endpoint, AppId aid, List<String> services, TraceLinks links) {
		return endpoint.requestUrl(aid.dropService(), Session.convertTailoringToString(services)).withQueryIfNotEmpty("from", ApiFormats.formatOrNull(links.getFrom()))
				.withQueryIfNotEmpty("to", ApiFormats.formatOrNull(links.getTo())).withoutProtocol().get();
	}

	private void sendReport(TaskReport report) {
		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);

		if (report.isSuccessful()) {
			LOGGER.info("Finished task {} successfully.", report.getTaskId());
		} else {
			LOGGER.warn("Finished task {} with errors.", report.getTaskId());
		}
	}

}
