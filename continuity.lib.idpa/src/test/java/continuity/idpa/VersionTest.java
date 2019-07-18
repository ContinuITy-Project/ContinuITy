package continuity.idpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.continuity.idpa.Version.fromString;

import java.io.IOException;

import org.continuity.idpa.Version;
import org.continuity.idpa.serialization.IdpaSerializationUtils;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class VersionTest {

	private ObjectMapper yamlMapper = IdpaSerializationUtils.getDefaultYamlObjectMapper();

	private ObjectMapper jsonMapper = IdpaSerializationUtils.getDefaultJsonObjectMapper();

	@Test
	public void testComparison() {
		assertThat(fromString("v1.2.3")).isEqualTo(fromString("1.2.3"));
		assertThat(fromString("v1.2")).isEqualTo(fromString("v1.2.0"));
		assertThat(fromString("v1.2")).isNotEqualTo(fromString("v1.3"));

		assertThat(fromString("v1.2.3")).isLessThan(fromString("v1.2.4"));
		assertThat(fromString("v1.2.4")).isGreaterThan(fromString("v1.2.3"));
		assertThat(fromString("v1.2")).isLessThan(fromString("v1.2.4"));
		assertThat(fromString("v1.2.4")).isGreaterThan(fromString("v1.2"));
	}

	@Test
	public void testYamlSerialization() throws NumberFormatException, IOException {
		testYaml("v1.2.3");
		testYaml("v1.2");
		testYaml("1.2.3");
		testYaml("v1");
		testYaml("42");
	}

	@Test
	public void testJsonSerialization() throws NumberFormatException, IOException {
		testJson("v1.2.3");
		testJson("v1.2");
		testJson("1.2.3");
		testJson("v1");
		testJson("42");
	}

	@Test
	public void testNormalizedString() {
		assertThat(fromString("v1").toNormalizedString()).isEqualTo("v1");
		assertThat(fromString("v1.0").toNormalizedString()).isEqualTo("v1");
		assertThat(fromString("v1.0.3.0.0").toNormalizedString()).isEqualTo("v1.0.3");
		assertThat(fromString("1.0").toNormalizedString()).isEqualTo("v1");
		assertThat(fromString("v0.1").toNormalizedString()).isEqualTo("v0.1");
		assertThat(fromString("v0.1.0").toNormalizedString()).isEqualTo("v0.1");
	}

	private void testYaml(String v) throws NumberFormatException, IOException {
		VersionHolder orig = new VersionHolder();
		orig.setVersion(fromString(v));
		String yaml = yamlMapper.writeValueAsString(orig);

		VersionHolder parsed = yamlMapper.readValue(yaml, VersionHolder.class);

		assertThat(parsed.getVersion()).isEqualTo(orig.getVersion());
	}

	private void testJson(String v) throws NumberFormatException, IOException {
		VersionHolder orig = new VersionHolder();
		orig.setVersion(fromString(v));
		String yaml = jsonMapper.writeValueAsString(orig);

		VersionHolder parsed = jsonMapper.readValue(yaml, VersionHolder.class);

		assertThat(parsed.getVersion()).isEqualTo(orig.getVersion());
	}

	private static class VersionHolder {

		private Version version;

		public Version getVersion() {
			return version;
		}

		public void setVersion(Version version) {
			this.version = version;
		}

	}

}
