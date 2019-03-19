package org.continuity.cli;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author Henning Schulz
 *
 */
@SpringBootApplication
public class ContinuityShell {

	public static void main(String[] args) throws Exception {
		new SpringApplicationBuilder(ContinuityShell.class).headless(false).run(args);
	}

}
