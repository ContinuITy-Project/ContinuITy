package continuity.idpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.PropertyOverrideKey.HttpEndpoint;
import org.junit.Before;
import org.junit.Test;

public class BasePathOverrideTest {

	private org.continuity.idpa.application.HttpEndpoint endpoint;

	@Before
	public void setup() {
		endpoint = new org.continuity.idpa.application.HttpEndpoint();
		endpoint.setPath("/foo/{}/bar");
	}

	@Test
	public void testPositive() {
		assertThat(testWith("1/baz")).isEqualTo("/baz/{}/bar");
		assertThat(testWith("2/baz")).isEqualTo("/baz/bar");
		assertThat(testWith("3/baz")).isEqualTo("/baz");

		assertThat(testWith("1/baz/42")).isEqualTo("/baz/42/{}/bar");
	}

	@Test()
	public void testNegative() {
		assertThat(testWith(null)).isEqualTo("/foo/{}/bar");
		assertThat(testWith("/baz")).isEqualTo("/foo/{}/bar");
		assertThat(testWith("abc/baz")).isEqualTo("/foo/{}/bar");
		assertThat(testWith("2")).isEqualTo("/foo/{}/bar");
	}

	private String testWith(String value) {
		PropertyOverride<HttpEndpoint> override = new PropertyOverride<>();
		override.setKey(HttpEndpoint.BASE_PATH);
		override.setValue(value);

		return override.resultingValue(endpoint);
	}

}
