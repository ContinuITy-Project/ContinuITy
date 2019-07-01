package org.continuity.session.logs.amqp;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.entities.config.ModularizationApproach;
import org.continuity.api.entities.config.ModularizationOptions;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.links.TraceLinks;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RestApi;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.session.logs.config.RabbitMqConfig;
import org.continuity.session.logs.extractor.ModularizedOPENxtraceSessionLogsExtractor;
import org.continuity.session.logs.extractor.OPENxtraceSessionLogsExtractor;
import org.continuity.session.logs.managers.ElasticsearchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SessionLogsAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionLogsAmqpHandler.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private ElasticsearchManager elasticManager;

	@Autowired
	private MixedStorage<SessionLogs> storage;

	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void createSessionLogs(TaskDescription task) throws IOException, TimeoutException {
		TaskReport report;
		AppId aid = task.getAppId();
		VersionOrTimestamp version = task.getVersion();
		TraceLinks links = task.getSource().getTraceLinks();

		boolean applyModularization = false;

		if (null != task.getModularizationOptions()) {
			ModularizationOptions modularizationOptions = task.getModularizationOptions();
			applyModularization = modularizationOptions.getModularizationApproach().equals(ModularizationApproach.SESSION_LOGS);
		}

		List<Trace> traces = elasticManager.readTraces(aid, version, links.getFrom(), links.getTo());

		String sessionLog;

		if (applyModularization) {
			LOGGER.info("Task {}: Creating modularized session logs for app-ids {}, version {}, and data from {} ...", task.getTaskId(), task.getModularizationOptions().getServices().keySet(),
					version, links.getFrom(), links.getTo());
			sessionLog = new ModularizedOPENxtraceSessionLogsExtractor(aid, restTemplate, task.getModularizationOptions().getServices()).getSessionLogs(traces);
		} else {
			LOGGER.info("Task {}: Creating session logs for app-id {}, version {}, and data from {} to {} ...", task.getTaskId(), aid, version, links.getFrom(), links.getTo());
			sessionLog = new OPENxtraceSessionLogsExtractor(aid, restTemplate).getSessionLogs(traces);
		}

		String id = storage.put(new SessionLogs(version, sessionLog), aid);
		String sessionLink = RestApi.SessionLogs.Sessions.GET.requestUrl(id).withoutProtocol().get();

		report = TaskReport.successful(task.getTaskId(), new LinkExchangeModel().getSessionLogsLinks().setLink(sessionLink).parent());

		LOGGER.info("Task {}: Session logs created.", task.getTaskId());

		amqpTemplate.convertAndSend(AmqpApi.Global.EVENT_FINISHED.name(), AmqpApi.Global.EVENT_FINISHED.formatRoutingKey().of(RabbitMqConfig.SERVICE_NAME), report);
	}

}
