package org.continuity.cli.commands;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.continuity.cli.config.PropertiesProvider;
import org.continuity.commons.format.CommonFormats;
import org.continuity.commons.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
@ShellComponent
public class WorkloadCommands {

	private static final String DEFAULT_DATE = "1970-01-01T00:00:00.000Z";

	private static final DateFormat DATE_FORMAT = CommonFormats.DATE_FORMAT;

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private RestTemplate restTemplate;

	@ShellMethod(key = { "create-dummy-workload" }, value = "Creates a dummy workload of a specified type and with a tag.")
	public ResponseEntity<String> createDummyWorkload(String type, String tag, @ShellOption(defaultValue = DEFAULT_DATE) String timestamp) {
		if (DEFAULT_DATE.equals(timestamp)) {
			timestamp = DATE_FORMAT.format(new Date());
		}

		Map<String, String> message = new HashMap<>();
		message.put("tag", tag);
		message.put("data", "dummy");
		message.put("timestamp", timestamp);

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
		return restTemplate.postForEntity(url + "/workloadmodel/" + type + "/create", message, String.class);
	}

}
