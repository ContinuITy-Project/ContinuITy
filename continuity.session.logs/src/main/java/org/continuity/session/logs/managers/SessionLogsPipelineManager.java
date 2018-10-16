package org.continuity.session.logs.managers;

import java.util.ArrayList;
import java.util.Map;

import org.continuity.api.entities.links.MeasurementDataLinkType;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.rest.InspectITRestClient;
import org.continuity.session.logs.converter.SessionConverterCSVData;
import org.continuity.session.logs.csv.ReadCSV;
import org.continuity.session.logs.entities.RowObject;
import org.continuity.session.logs.extractor.InspectITSessionLogsExtractor;
import org.continuity.session.logs.extractor.ModularizedOPENxtraceSessionLogsExtractor;
import org.continuity.session.logs.extractor.OPENxtraceSessionLogsExtractor;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import open.xtrace.OPENxtraceUtils;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;

/**
 *
 * @author Alper Hi, Tobias Angerstein, Henning Schulz
 *
 */
public class SessionLogsPipelineManager {

	private String cmrConfig;
	private String link;
	private String tag;

	private final RestTemplate eurekaRestTemplate;

	private final RestTemplate plainRestTemplate;

	public SessionLogsPipelineManager(String link, String tag, RestTemplate plainRestTemplate, RestTemplate eurekaRestTemplate) {
		this.link = link;
		this.tag = tag;
		if(ResourceUtils.isUrl(link)) {
			UriComponents uri = UriComponentsBuilder.fromHttpUrl(link).build();
			cmrConfig = uri.getHost() + ":" + uri.getPort();
		}
		this.eurekaRestTemplate = eurekaRestTemplate;
		this.plainRestTemplate = plainRestTemplate;
	}

	/**
	 * Runs the pipeline Based on the environment variable, different input data is used.
	 *
	 * @return
	 */
	public String runPipeline(MeasurementDataLinkType measurementType) {
		switch(measurementType) {
		case INSPECTIT:
			return new InspectITSessionLogsExtractor(tag, eurekaRestTemplate, cmrConfig).getSessionLogs(getInvocationSequences());
		case OPEN_XTRACE:
			LinkExchangeModel source = new LinkExchangeModel();
			source.getMeasurementDataLinks().setLink(link);
			return new OPENxtraceSessionLogsExtractor(tag, eurekaRestTemplate).getSessionLogs(OPENxtraceUtils.getOPENxtraces(source, plainRestTemplate));
		case CSV:
			return getSessionLogsFromCSV(this.link);
		default:
			throw new RuntimeException("The given measurement cannot be resolved!");
		}
	}
	
	/**
	 * Generates session logs from CSV file.
	 * @param link
	 * @return
	 */
	private String getSessionLogsFromCSV(String link) {	
		ReadCSV rcsv = new ReadCSV();
		ArrayList<RowObject> dataList = rcsv.readDataFromCSV(link);
		SessionConverterCSVData csvsc = new SessionConverterCSVData();
		String sessionLogs = csvsc.createSessionLogsFromCSV(dataList);
		return sessionLogs;
	}

	/**
	 * Runs the pipeline using the session logs modularization. Based on the environment variable,
	 * different input data is used.
	 * 
	 *
	 * @return
	 */
	public String runPipeline(MeasurementDataLinkType measurementType, Map<String, String> hostnames) {
		if (measurementType.equals(MeasurementDataLinkType.OPEN_XTRACE)) {
			LinkExchangeModel source = new LinkExchangeModel();
			source.getMeasurementDataLinks().setLink(link);
			return new ModularizedOPENxtraceSessionLogsExtractor(tag, eurekaRestTemplate, hostnames).getSessionLogs(OPENxtraceUtils.getOPENxtraces(source, plainRestTemplate));
		} else {
			throw new UnsupportedOperationException("Modularization of the session logs is currently only supported with open.XTRACE as source");
		}
	}

	/**
	 * Gets session logs without using the invocation sequences of the CMR.
	 *
	 * @return
	 */
	private Iterable<InvocationSequenceData> getInvocationSequences() {
		InspectITRestClient fetcher = new InspectITRestClient(cmrConfig);
		MultiValueMap<String, String> uriParameters = UriComponentsBuilder.fromHttpUrl(this.link).build().getQueryParams();
		Iterable<InvocationSequenceData> invocationSequenceIterable = fetcher.fetchAll(0, uriParameters.getFirst("fromDate"), uriParameters.getFirst("toDate"));
		return invocationSequenceIterable;
	}
}
