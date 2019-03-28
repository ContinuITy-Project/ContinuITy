package org.continuity.commons.idpa;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.visitor.FindBy;

/**
 * Collects parameter values for one endpoint and takes care of the correlation of the values.
 *
 * @author Henning Schulz
 *
 */
public class ParameterValueCollector {

	private static final String EMPTY_VALUE = "";

	private final HttpEndpoint endpoint;

	private final Map<HttpParameter, List<String>> valuesPerParam = new LinkedHashMap<>();

	public ParameterValueCollector(HttpEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public void collectFromPath(String path) {
		UrlPartParameterExtractor extractor = new UrlPartParameterExtractor(endpoint.getPath(), path);

		while (extractor.hasNext()) {
			storeParamAndValue(extractor.nextParameter(), extractor.currentValue());
		}
	}

	public void collectFromQueryString(String query) {
		if ((query == null) || query.isEmpty()) {
			return;
		}

		for (String paramString : query.split("&")) {
			String[] paramAndValue = paramString.split("=");

			if (paramAndValue.length >= 2) {
				String param = paramAndValue[0];
				String value = paramAndValue[1];

				storeParamAndValue(param, value);
			}
		}

		balance();
	}

	public Map<HttpParameter, List<String>> getValuesPerParam() {
		return valuesPerParam;
	}

	public HttpEndpoint getEndpoint() {
		return endpoint;
	}

	private void balance() {
		int maxSize = valuesPerParam.values().stream().mapToInt(List::size).max().orElse(0);

		if (maxSize > 0) {
			for (List<String> list : valuesPerParam.values()) {
				while (list.size() < maxSize) {
					list.add(0, EMPTY_VALUE);
				}
			}
		}
	}

	private void storeParamAndValue(String param, String value) {
		HttpParameter foundParam = FindBy.find(p -> Objects.equals(param, p.getName()), HttpParameter.class).in(endpoint).getFound();

		if (foundParam != null) {
			List<String> valueList = valuesPerParam.get(foundParam);

			if (valueList == null) {
				valueList = new LinkedList<>();
				valuesPerParam.put(foundParam, valueList);
			}

			valueList.add(value);
		}
	}

}
