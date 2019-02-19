package org.continuity.request.rates.transform;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.continuity.commons.utils.StringUtils;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.request.rates.entities.RequestRecord;
import org.continuity.request.rates.model.RequestFrequency;
import org.continuity.request.rates.model.RequestRatesModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RequestRatesCalculator {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestRatesCalculator.class);

	private static final String UNKNOWN_ENDPOINT = "UNKNOWN";

	protected abstract HttpEndpoint mapToEndpoint(RequestRecord record);

	public abstract boolean useNames();

	public RequestRatesModel calculate(List<RequestRecord> records) {
		RequestRatesModel model = new RequestRatesModel();

		sortRecords(records);

		List<RequestFrequency> mix;

		if (useNames()) {
			mix = calculateAbsoluteMixUsingNames(records);
		} else {
			mix = calculateAbsoluteMixUsingApplication(records);
		}

		double overallNumRequests = getOverallNumberOfRequests(mix);
		relativizeMix(mix, overallNumRequests);

		model.setRequestsPerMinute(overallNumRequests / calculateDuration(records));
		model.setMix(mix);

		checkMix(mix);

		return model;
	}

	private double getOverallNumberOfRequests(List<RequestFrequency> absoluteMix) {
		return absoluteMix.stream().map(RequestFrequency::getFreq).reduce(Double::sum).get();
	}

	private void relativizeMix(List<RequestFrequency> absoluteMix, double overallNumRequests) {
		absoluteMix.forEach(freq -> freq.setFreq(freq.getFreq() / overallNumRequests));
	}

	private void checkMix(List<RequestFrequency> mix) {
		double sum = mix.stream().map(RequestFrequency::getFreq).reduce(Double::sum).get();

		if (Math.abs(sum - 1.0) > 0.0001) {
			LOGGER.warn("The frequencies of the mix don't sum up to 1! The sum is {}.", sum);
		}
	}

	private void sortRecords(List<RequestRecord> records) {
		Collections.sort(records, (RequestRecord a, RequestRecord b) -> {
			int startTimeComparison = a.getStartDate().compareTo(b.getStartDate());

			if (startTimeComparison != 0) {
				return startTimeComparison;
			} else {
				return a.getEndDate().compareTo(b.getEndDate());
			}
		});
	}

	private long calculateDuration(List<RequestRecord> records) {
		Date startDate = records.get(0).getStartDate();
		Date endDate = records.get(records.size() - 1).getStartDate();

		return TimeUnit.MINUTES.convert(endDate.getTime() - startDate.getTime(), TimeUnit.MILLISECONDS);
	}

	private List<RequestFrequency> calculateAbsoluteMixUsingApplication(List<RequestRecord> records) {
		return records.stream().map(this::mapToEndpoint).filter(Objects::nonNull).collect(Collectors.groupingBy(HttpEndpoint::getId)).entrySet().stream()
				.map(entry -> new RequestFrequency(entry.getValue().size(), entry.getValue().get(0))).collect(Collectors.toList());
	}

	private List<RequestFrequency> calculateAbsoluteMixUsingNames(List<RequestRecord> records) {
		return records.stream().map(this::replaceNullName).collect(Collectors.groupingBy(RequestRecord::getName)).entrySet().stream()
				.map(entry -> new RequestFrequency(entry.getValue().size(), aggregateRequests(entry.getValue()))).collect(Collectors.toList());
	}

	private RequestRecord replaceNullName(RequestRecord record) {
		if (record.getName() == null) {
			record.setName(UNKNOWN_ENDPOINT);
		}

		return record;
	}

	private Endpoint<?> aggregateRequests(List<RequestRecord> records) {
		HttpEndpoint endpoint = new HttpEndpoint();

		endpoint.setId(getFirst(records, RequestRecord::getName));
		endpoint.setDomain(getFirst(records, RequestRecord::getDomain));
		endpoint.setPort(getFirst(records, RequestRecord::getPort));
		endpoint.setPath(getFirst(records, RequestRecord::getPath));
		endpoint.setMethod(getFirst(records, RequestRecord::getMethod));
		endpoint.setProtocol(getFirst(records, RequestRecord::getProtocol));
		endpoint.setEncoding(getFirst(records, RequestRecord::getEncoding));

		endpoint.setHeaders(extractHeaders(records));

		endpoint.setParameters(extractHttpParameters(records));
		setParameterIds(endpoint);

		return endpoint;
	}

	private <T> T getFirst(List<RequestRecord> records, Function<RequestRecord, T> getter) {
		for (RequestRecord rec : records) {
			T elem = getter.apply(rec);
			if (elem != null) {
				return elem;
			}
		}

		return null;
	}

	private List<String> extractHeaders(List<RequestRecord> records) {
		return records.stream().map(RequestRecord::getHeaders).filter(Objects::nonNull).flatMap(List::stream).distinct().collect(Collectors.toList());
	}

	private List<HttpParameter> extractHttpParameters(List<RequestRecord> records) {
		return records.stream().map(RequestRecord::getHeaders).filter(Objects::nonNull).flatMap(List::stream).distinct().map(name -> {
			HttpParameter param = new HttpParameter();

			if (name.startsWith("_BODY")) {
				param.setParameterType(HttpParameterType.BODY);
			} else if (name.startsWith("URL_PART")) {
				param.setName(name.substring("URL_PART".length()));
				param.setParameterType(HttpParameterType.URL_PART);
			} else {
				param.setName(name);
				param.setParameterType(HttpParameterType.REQ_PARAM);
			}

			return param;
		}).collect(Collectors.toList());
	}

	private void setParameterIds(HttpEndpoint interf) {
		final Set<String> ids = new HashSet<>();

		for (HttpParameter param : interf.getParameters()) {
			String id = StringUtils.formatAsId(true, interf.getId(), param.getName(), param.getParameterType().toString());
			String origId = id;
			int i = 2;

			while (ids.contains(id)) {
				id = origId + "_" + i++;
			}

			ids.add(id);
			param.setId(id);
		}
	}

}
