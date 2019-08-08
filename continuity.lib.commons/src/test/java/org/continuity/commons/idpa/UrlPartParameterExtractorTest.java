package org.continuity.commons.idpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.junit.Test;

public class UrlPartParameterExtractorTest {

	@Test
	public void test() {
		// * = does not match
		test("/foo/bar/{id}", "/foo/bar/42", Arrays.asList("id"), Arrays.asList("42"), "id");
		test("/foo/bar/{id}", "/foo/bar/{}", Arrays.asList("id"), Arrays.asList("{}"), "id");
		test("/foo/bar/{id}", "foo/bar/", Arrays.asList("id"), Collections.singletonList(""), "id");
		test("/foo/bar/{id}", "/foo/bar/42/baz", Arrays.asList("id"), Collections.singletonList(null), "id"); // *
		test("/foo/{bar}/{id}", "foo/bar/42", Arrays.asList("bar", "id"), Arrays.asList("bar", "42"), "bar", "id");
		test("/foo/{bar}/{id}", "foo/bar", Arrays.asList("bar", "id"), Arrays.asList(null, null), "bar", "id"); // *

		test("/foo/bar/{rest:*}", "/foo/bar/42", Arrays.asList("rest"), Arrays.asList("42"), "rest");
		test("/foo/bar/{rest:*}", "/foo/bar/42/baz", Arrays.asList("rest"), Arrays.asList("42/baz"), "rest");
		test("/foo/bar/{rest:*}", "/foo/bar/", Arrays.asList("rest"), Collections.singletonList(""), "rest");
		test("/foo/{id}/{rest:*}", "/foo/bar/42/baz", Arrays.asList("id", "rest"), Arrays.asList("bar", "42/baz"), "id", "rest");
		test("/foo/{id}/{rest:*}", "/foo/bar/", Arrays.asList("id", "rest"), Arrays.asList("bar", ""), "id", "rest");

		test("/foo/{id}/{rest:42.*}", "/foo/bar/42/baz", Arrays.asList("id", "rest"), Arrays.asList("bar", "42/baz"), "id", "rest");

		test("/foo/bar/id", "/foo/bar/42", Arrays.asList(), Arrays.asList());
	}

	private void test(String pattern, String path, List<String> expectedParams, List<String> expectedValues, String... params) {
		HttpEndpoint endpoint = new HttpEndpoint();
		endpoint.setPath(pattern);
		endpoint.setParameters(new ArrayList<>());

		for (String p : params) {
			HttpParameter param = new HttpParameter();
			param.setName(p);
			param.setParameterType(HttpParameterType.URL_PART);
			endpoint.addParameter(param);
		}

		UrlPartParameterExtractor extractor = new UrlPartParameterExtractor(endpoint, path);

		List<String> extractedParams = new ArrayList<>();
		List<String> extractedValues = new ArrayList<>();

		while (extractor.hasNext()) {
			extractedParams.add(extractor.nextParameter());
			extractedValues.add(extractor.currentValue());
		}

		assertThat(extractedParams).as("Extracted parameter names").isEqualTo(expectedParams);
		assertThat(extractedValues).as("Extracted parameter values").isEqualTo(expectedValues);
	}

}
