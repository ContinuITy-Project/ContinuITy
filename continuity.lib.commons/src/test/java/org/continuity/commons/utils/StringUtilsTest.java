package org.continuity.commons.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.Test;

public class StringUtilsTest {

	private static final int STRING_LENGTH = 42;

	private final Random random = new Random(42);

	@Test
	public void testWithoutShortening() {
		assertThat(StringUtils.formatAsId(false, "myId_1")).isEqualTo("myId_1");
		assertThat(StringUtils.formatAsId(false, "my.id.42")).isEqualTo("my_id_42");
		assertThat(StringUtils.formatAsId(false, "myId.1")).isEqualTo("myId_1");
		assertThat(StringUtils.formatAsId(false, "myId/foo/bar/")).isEqualTo("myId_foo_bar");
		assertThat(StringUtils.formatAsId(false, "myId", "foo", "bar")).isEqualTo("myId_foo_bar");
		assertThat(StringUtils.formatAsId(false, "myId", "42", "1")).isEqualTo("myId_42_1");
		assertThat(StringUtils.formatAsId(false, "1", "id")).isEqualTo("1_id");
	}

	@Test
	public void testWithShortening() {
		assertThat(StringUtils.formatAsId(true, "myId_1")).isEqualTo("myId_1");
		assertThat(StringUtils.formatAsId(true, "my.id.42")).isEqualTo("m_i_42");
		assertThat(StringUtils.formatAsId(true, "myId.1")).isEqualTo("m_1");
		assertThat(StringUtils.formatAsId(true, "myId/foo/bar/")).isEqualTo("m_f_bar");
		assertThat(StringUtils.formatAsId(true, "myId", "foo", "bar")).isEqualTo("myId_foo_bar");
		assertThat(StringUtils.formatAsId(true, "my.Id", "foo.bar/42", "abc/xy")).isEqualTo("m_Id_f_b_42_a_xy");
		assertThat(StringUtils.formatAsId(true, "myId", "42", "1")).isEqualTo("myId_42_1");
		assertThat(StringUtils.formatAsId(true, "1", "id")).isEqualTo("1_id");
	}

	@Test
	public void testRandomly() {
		for (int i = 0; i < 1000; i++) {
			String unformatted = randomString();
			String longId = StringUtils.formatAsId(false, unformatted);
			String shortId = StringUtils.formatAsId(true, unformatted);

			assertThat(longId).matches("[a-zA-Z0-9_]+");
			assertThat(shortId).matches("[a-zA-Z0-9_]+");
		}
	}

	private String randomString() {
		char[] chars = new char[STRING_LENGTH];

		for (int i = 0; i < STRING_LENGTH; i++) {
			chars[i] = (char) random.nextInt(0x7F);
		}

		return new String(chars);
	}

}
