package org.continuity.cli.commands;

import java.util.HashMap;
import java.util.Map;

import org.continuity.cli.config.PropertiesProvider;
import org.continuity.commons.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
@ShellComponent
public class WorkloadCommands {

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private RestTemplate restTemplate;

	@ShellMethod(key = { "create-dummy-workload" }, value = "Creates a dummy workload of a specified type and with a tag.")
	public ResponseEntity<String> createDummyWorkload(String type, String tag) {
		Map<String, String> message = new HashMap<>();
		message.put("tag", tag);
		message.put("data", "dummy");

		String url = WebUtils.addProtocolIfMissing(propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL));
		return restTemplate.postForEntity(url + "/workloadmodel/" + type + "/create", message, String.class);
	}

}
