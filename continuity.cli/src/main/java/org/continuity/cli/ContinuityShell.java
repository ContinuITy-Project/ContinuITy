package org.continuity.cli;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.jline.PromptProvider;

/**
 * @author Henning Schulz
 *
 */
@SpringBootApplication
public class ContinuityShell {

	public static void main(String[] args) throws Exception {
		new SpringApplicationBuilder(ContinuityShell.class).headless(false).run(args);
	}

	@Bean
	public PromptProvider myPromptProvider() {
		return () -> new AttributedString("continuity:>", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
	}

}
