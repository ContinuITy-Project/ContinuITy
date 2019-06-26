package org.continuity.commons.idpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class UrlPartParameterExtractorTest {

	@Test
	public void test() {
		test("/foo/bar/{id}", "/foo/bar/42", Arrays.asList("id"), Arrays.asList("42"));
		test("/foo/bar/{id}", "/foo/bar/{}", Arrays.asList("id"), Arrays.asList("{}"));
		test("/foo/bar/{id}", "foo/bar/", Arrays.asList("id"), Collections.singletonList(null));
		test("/foo/bar/{id}", "/foo/bar/42/baz", Arrays.asList("id"), Arrays.asList("42"));
		test("/foo/{bar}/{id}", "foo/bar/42", Arrays.asList("bar", "id"), Arrays.asList("bar", "42"));
		test("/foo/{bar}/{id}", "foo/bar", Arrays.asList("bar", "id"), Arrays.asList("bar", null));

		test("/foo/bar/{rest:*}", "/foo/bar/42", Arrays.asList("rest"), Arrays.asList("42"));
		test("/foo/bar/{rest:*}", "/foo/bar/42/baz", Arrays.asList("rest"), Arrays.asList("42/baz"));
		test("/foo/bar/{rest:*}", "/foo/bar/", Arrays.asList("rest"), Collections.singletonList(null));
		test("/foo/{id}/{rest:*}", "/foo/bar/42/baz", Arrays.asList("id", "rest"), Arrays.asList("bar", "42/baz"));
		test("/foo/{id}/{rest:*}", "/foo/bar/", Arrays.asList("id", "rest"), Arrays.asList("bar", null));

		test("/foo/bar/id", "/foo/bar/42", Arrays.asList(), Arrays.asList());
	}

	private void test(String pattern, String path, List<String> expectedParams, List<String> expectedValues) {
		UrlPartParameterExtractor extractor = new UrlPartParameterExtractor(pattern, path);

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
