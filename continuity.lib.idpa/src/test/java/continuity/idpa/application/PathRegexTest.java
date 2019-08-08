package continuity.idpa.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.idpa.application.HttpEndpoint;
import org.junit.Test;

public class PathRegexTest {

	@Test
	public void test() {
		test("/foo/{bar}/{baz:baz.*}", "/foo/bar/baz", true);
		test("/foo/{bar}/{baz:baz.*}", "/foo/42/bazz", true);
		test("/foo/{bar}/{baz:baz.*}", "/foo/bar/baz/baz/123", true);
		test("/foo/{bar}/{baz:baz.*}", "/foo/bar/bar/baz", false);
		test("/foo/{bar}/{baz:baz.*}", "/foo/bar", false);
		test("/foo/{bar}/{baz:baz.*}", "/foo/baz", false);

		test("/foo/bar", "/foo/bar", true);

		test("/foo/bar/{baz:*}", "/foo/bar", false);
		test("/foo/bar/{baz:*}", "/foo/bar/baz", true);
		test("/foo/bar/{baz:*}", "/foo/bar/baz/123", true);

		test("/foo/{bar:bar.*}", "/foo/bar", true);
		test("/foo/{bar:bar.*}", "/foo/bar/baz", true);
		test("/foo/{bar:bar.*}", "/foo/bar/baz/123", true);
		test("/foo/{bar:bar.*}", "/foo/baz", false);

		test("/foo/{bar:[0-9]+}/{baz:*}", "/foo/123", false);
		test("/foo/{bar:[0-9]+}/{baz:*}", "/foo/123/baz", true);
		test("/foo/{bar:[0-9]+}/{baz:*}", "/foo/42/baz/123", true);
		test("/foo/{bar:[0-9]+}/{baz:*}", "/foo/bar/baz", false);
	}

	private void test(String pathPattern, String path, boolean match) {
		HttpEndpoint endpoint = new HttpEndpoint();
		endpoint.setPath(pathPattern);

		String regex = endpoint.getPathAsRegex();

		if (match) {
			assertThat(path.matches(regex)).as("Expecting " + path + " to match " + regex).isTrue();
		} else {
			assertThat(path.matches(regex)).as("Expecting " + path + " not to match " + regex).isFalse();
		}
	}

}
