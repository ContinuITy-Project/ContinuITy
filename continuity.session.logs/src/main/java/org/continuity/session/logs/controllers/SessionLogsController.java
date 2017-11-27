package org.continuity.session.logs.controllers;

import org.continuity.session.logs.managers.SessionLogsPipelineManager;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 * @author Alper Hi
 *
 */
@RestController
public class SessionLogsController {

	/**
	 * @param link
	 * @return
	 */
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public String update(@RequestBody String link) {

		SessionLogsPipelineManager manager = new SessionLogsPipelineManager(link);

		String sessionLog = manager.runPipeline();

		return sessionLog;
	}
}
