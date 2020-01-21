package org.continuity.cobra.amqp;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.continuity.api.amqp.AmqpApi;
import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.artifact.ForecastIntensityRecord;
import org.continuity.api.entities.artifact.session.Session;
import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.BehaviorModelType;
import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.entities.order.TailoringApproach;
import org.continuity.api.entities.report.TaskError;
import org.continuity.api.entities.report.TaskReport;
import org.continuity.api.rest.RequestBuilder;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestEndpoint;
import org.continuity.cobra.config.RabbitMqConfig;
import org.continuity.cobra.entities.ForecastTimerange;
import org.continuity.cobra.entities.ForecasticInput;
import org.continuity.cobra.entities.ForecasticResult;
import org.continuity.cobra.entities.TimedContextRecord;
import org.continuity.cobra.entities.TypeAndProperties;
import org.continuity.cobra.managers.ElasticsearchIntensityManager;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.cobra.managers.ElasticsearchTraceManager;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.commons.utils.TailoringUtils;
import org.continuity.dsl.WorkloadDescription;
import org.continuity.dsl.schema.IgnoreByDefaultValue;
import org.continuity.dsl.timeseries.IntensityRecord;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PreparationAmqpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreparationAmqpHandler.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ElasticsearchSessionManager elasticSessionManager;

	@Autowired
	private ElasticsearchTraceManager elasticTraceManager;

	@Autowired
	private ElasticsearchIntensityManager elasticIntensityManager;

	@Autowired
	private ConfigurationProvider<CobraConfiguration> configProvider;

	@Autowired
	private MixedStorage<List<ForecastIntensityRecord>> intensityStorage;

	@RabbitListener(queues = RabbitMqConfig.TASK_CREATE_QUEUE_NAME)
	public void prepareInitialData(TaskDescription task) throws IOException, TimeoutException {
		WorkloadDescription description = task.getWorkloadDescription();

		LOGGER.info("Processing task {}...", task.getTaskId());

		if (description == null) {
			sendReport(TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE));
			return;
		}

		List<String> tailoring = extractServices(task);

		List<IntensityRecord> intensities = readIntensities(task.getAppId(), tailoring, description);
		List<ForecastTimerange> ranges = extractRanges(task.getAppId(), intensities);

		LOGGER.warn("Currently, only the past traces, sessions, or behavior model are selected!");

		LOGGER.info("Task {}: Get {} in ranges {}...", task.getTaskId(), task.getTarget().toPrettyString(), ranges);

		TaskReport report;

		switch (task.getTarget()) {
		case TRACES:
			report = createTraceLink(task, ranges);
			break;
		case SESSIONS:
			report = createSessionLink(task, ranges);
			break;
		case BEHAVIOR_MODEL:
			report = createBehaviorLink(task, ranges);
			break;
		default:
			LOGGER.error("Task {}: Cannot generate {}!", task.getTaskId(), task.getTarget().toPrettyString());
			report = TaskReport.error(task.getTaskId(), TaskError.ILLEGAL_TYPE);
			break;
		}

		if (report.isSuccessful()) {
			report.getResult().setIntensity(doForecast(task, ranges, intensities));
		}

		sendReport(report);
	}

	private List<IntensityRecord> readIntensities(AppId aid, List<String> tailoring, WorkloadDescription description) throws IOException, TimeoutException {
		CobraConfiguration config = configProvider.getConfiguration(aid);
		Duration resolution = config.getIntensity().getResolution();
		ZoneId timeZone = config.getTimeZone();

		elasticIntensityManager.fillIntensities(aid, tailoring, description.getMinDate(), description.getMaxDate(), resolution, timeZone);

		List<IntensityRecord> intensities = elasticIntensityManager.readDescribedIntensities(aid, tailoring, description, timeZone);

		if (description.requiresPostprocessing()) {
			List<LocalDateTime> appliedDates = intensities.stream().map(IntensityRecord::getTimestamp).map(t -> Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDateTime())
					.collect(Collectors.toList());

			List<IntensityRecord> additional = elasticIntensityManager.readPostprocessing(aid, tailoring, description, appliedDates, resolution);

			intensities.addAll(additional);
			intensities.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
		}

		return intensities;
	}

	private List<ForecastTimerange> extractRanges(AppId aid, List<IntensityRecord> intensities) {
		long resolution = configProvider.getConfiguration(aid).getIntensity().getResolution().toMillis();

		List<ForecastTimerange> ranges = new ArrayList<>();
		IntensityRecord rangeStart = intensities.get(0);
		IntensityRecord last = null;

		for (IntensityRecord next : intensities) {
			if ((last != null) && ((next.getTimestamp() - last.getTimestamp()) > resolution)) {
				ranges.add(new ForecastTimerange(rangeStart.getTimestamp(), last.getTimestamp()));
				rangeStart = next;
			}

			last = next;
		}

		ranges.add(new ForecastTimerange(rangeStart.getTimestamp(), last.getTimestamp()));

		return ranges;
	}

	private TaskReport createTraceLink(TaskDescription task, List<ForecastTimerange> ranges) throws IOException {
		long count = 0;

		if (ranges.isEmpty()) {
			count = elasticTraceManager.countTraces(task.getAppId(), null, null, null);
		} else {
			for (ForecastTimerange range : ranges) {
				count += elasticTraceManager.countTraces(task.getAppId(), null, new Date(range.getFrom()), new Date(range.getTo()));
			}
		}

		if (count == 0) {
			LOGGER.error("Task {}: There are no such traces available!", task.getTaskId());
			return TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			ArtifactExchangeModel artifacts = new ArtifactExchangeModel();
			artifacts.getTraceLinks().setLink(formatTraceLink(task.getAppId(), task.getVersion(), ranges));
			return TaskReport.successful(task.getTaskId(), artifacts);
		}
	}

	private TaskReport createSessionLink(TaskDescription task, List<ForecastTimerange> ranges) throws IOException {
		List<String> services = extractServices(task);

		long count = 0;

		if (ranges.isEmpty()) {
			count = elasticSessionManager.countSessionsInRange(task.getAppId(), null, services, null, null);
		} else {
			for (ForecastTimerange range : ranges) {
				count += elasticSessionManager.countSessionsInRange(task.getAppId(), null, services, new Date(range.getFrom()), new Date(range.getTo()));
			}
		}

		if (count == 0) {
			LOGGER.error("Task {}: There are no such sessions available!", task.getTaskId());
			return TaskReport.error(task.getTaskId(), TaskError.MISSING_SOURCE);
		} else {
			ArtifactExchangeModel artifacts = new ArtifactExchangeModel();
			artifacts.getSessionLinks().setSimpleLink(formatSessionLink(RestApi.Cobra.Sessions.GET_SIMPLE, task.getAppId(), services, ranges))
					.setExtendedLink(formatSessionLink(RestApi.Cobra.Sessions.GET_EXTENDED, task.getAppId(), services, ranges));
			return TaskReport.successful(task.getTaskId(), artifacts);
		}
	}

	private TaskReport createBehaviorLink(TaskDescription task, List<ForecastTimerange> ranges) throws IOException {
		OptionalLong before = ranges.stream().mapToLong(ForecastTimerange::getTo).max();

		RequestBuilder reqBuilder = RestApi.Cobra.BehaviorModel.GET_LATEST.requestUrl(task.getAppId(), Session.convertTailoringToString(extractServices(task)));

		if (before.isPresent()) {
			reqBuilder.withQuery("before", Long.toString(before.getAsLong()));
		}

		ArtifactExchangeModel artifacts = new ArtifactExchangeModel().getBehaviorModelLinks().setLink(reqBuilder.withoutProtocol().get()).setType(BehaviorModelType.MARKOV_CHAIN).parent();

		return TaskReport.successful(task.getTaskId(), artifacts);
	}

	private String formatSessionLink(RestEndpoint endpoint, AppId aid, List<String> services, List<ForecastTimerange> ranges) {
		RequestBuilder reqBuilder = endpoint.requestUrl(aid.dropService(), Session.convertTailoringToString(services));

		for (ForecastTimerange range : ranges) {
			reqBuilder.withQuery("from", ApiFormats.formatMillisAsDate(range.getFrom())).withQuery("to", ApiFormats.formatMillisAsDate(range.getTo()));
		}

		return reqBuilder.withoutProtocol().get();
	}

	private String formatTraceLink(AppId aid, VersionOrTimestamp version, List<ForecastTimerange> ranges) {
		RequestBuilder reqBuilder;

		if (version == null) {
			reqBuilder = RestApi.Cobra.MeasurementData.GET.requestUrl(aid);
		} else {
			reqBuilder = RestApi.Cobra.MeasurementData.GET_VERSION.requestUrl(aid, version);
		}

		for (ForecastTimerange range : ranges) {
			reqBuilder.withQuery("from", ApiFormats.formatMillisAsDate(range.getFrom())).withQuery("to", ApiFormats.formatMillisAsDate(range.getTo()));
		}

		return reqBuilder.withoutProtocol().get();
	}

	private String doForecast(TaskDescription task, List<ForecastTimerange> ranges, List<IntensityRecord> intensities) {
		LOGGER.info("Task {}: Doing forecast...", task.getTaskId());

		CobraConfiguration config = configProvider.getConfiguration(task.getAppId());
		WorkloadDescription description = task.getWorkloadDescription();

		IgnoreByDefaultValue ignore = config.getContext().ignoreByDefault();

		for (IntensityRecord record : intensities) {
			if (record.getContext() != null) {
				if (record.getContext().getNumeric() != null) {
					record.getContext().getNumeric().removeIf(v -> ignore.ignore(config.getContext().getVariables().get(v.getName()).getIgnoreByDefault()));

					if (record.getContext().getNumeric().isEmpty()) {
						record.getContext().setNumeric(null);
					}
				}

				if (record.getContext().getString() != null) {
					record.getContext().getString().removeIf(v -> ignore.ignore(config.getContext().getVariables().get(v.getName()).getIgnoreByDefault()));

					if (record.getContext().getString().isEmpty()) {
						record.getContext().setString(null);
					}
				}

				if (record.getContext().getBoolean() != null) {
					record.getContext().getBoolean().removeIf(v -> ignore.ignore(config.getContext().getVariables().get(v).getIgnoreByDefault()));

					if (record.getContext().getBoolean().isEmpty()) {
						record.getContext().setBoolean(null);
					}
				}
			}
		}

		description.adjustContext(intensities, config.getTimeZone());

		ForecasticInput input = new ForecasticInput().setAppId(task.getAppId()).setTailoring(extractServices(task)).setApproach(task.getOptions().getForecastApproachOrDefault());

		input.setRanges(ranges).setResolution(config.getIntensity().getResolution().toMillis());

		input.setContext(intensities.stream().map(TimedContextRecord::fromIntensity).filter(Objects::nonNull).collect(Collectors.toList()));

		input.setAggregation(TypeAndProperties.fromTypedProperties(description.getAggregation()));

		input.setAdjustments(
				Optional.ofNullable(description.getAdjustments()).map(l -> l.stream().map(TypeAndProperties::fromTypedProperties).collect(Collectors.toList())).orElse(Collections.emptyList()));

		ForecasticResult result = restTemplate.postForObject(RestApi.Forecastic.FORECAST.requestUrl().get(), input, ForecasticResult.class);
		LOGGER.info("Task {}: Received {} intensity records from forecastic.", task.getTaskId(), result.getIntensities().size());

		String id = intensityStorage.put(result.getIntensities(), task.getAppId(), task.isLongTermUse());
		LOGGER.info("Task {}: Stored intensity records to storage with ID {}.", task.getTaskId(), id);

		return RestApi.Cobra.Intensity.GET_FOR_ID.requestUrl(id).withoutProtocol().get();
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
