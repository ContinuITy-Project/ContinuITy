package org.continuity.cli.commands;

import org.continuity.cli.exception.CliException;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.idpa.AppId;
import org.jline.utils.AttributedString;
import org.springframework.shell.ParameterMissingResolutionException;
import org.springframework.web.client.HttpClientErrorException;

/**
 *
 * @author Henning Schulz
 *
 */
public abstract class AbstractCommands {

	private final CliContextManager contextManager;

	protected AbstractCommands(CliContextManager contextManager) {
		this.contextManager = contextManager;
	}

	protected AttributedString execute(Executor impl) throws Exception {
		try {
			return impl.execute();
		} catch (ParameterMissingResolutionException e) {
			return new ResponseBuilder().error("Missing parameter: ").error(e.getMessage()).build();
		} catch (IllegalArgumentException e) {
			return new ResponseBuilder().error("Illegal argument: ").error(e.getMessage()).build();
		} catch (CliException e) {
			return e.getAttributedMessage();
		} catch (HttpClientErrorException e) {
			return new ResponseBuilder().error("Bad request: ").boldError(e.getRawStatusCode()).boldError(" (").boldError(e.getStatusCode().getReasonPhrase()).boldError(")").error(": ")
					.error(e.getResponseBodyAsString()).build();
		}
	}

	protected AttributedString executeWithAppId(String appId, AppIdExecutor impl) throws Exception {
		return execute(() -> impl.execute(contextManager.getAppIdOrFail(appId)));
	}

	protected AttributedString executeWithCurrentAppId(AppIdExecutor impl) throws Exception {
		AppId aid = contextManager.getCurrentAppId();

		if (aid == null) {
			return errorMissingAppId();
		} else {
			return execute(() -> impl.execute(aid));
		}
	}

	protected AttributedString executeWithAppIdAndVersion(String appId, String version, AppIdAnVersionExecutor impl) throws Exception {
		return execute(() -> impl.execute(contextManager.getAppIdOrFail(appId), contextManager.getVersionOrFail(version)));
	}

	protected AttributedString errorMissingAppId() {
		return new ResponseBuilder().error("Missing an app-id! Please specify one using ").boldError("app-id <your_id>").normal("!").build();
	}

	public static interface Executor {

		AttributedString execute() throws Exception;

	}

	public static interface AppIdExecutor {

		AttributedString execute(AppId aid) throws Exception;

	}

	public static interface AppIdAnVersionExecutor {

		AttributedString execute(AppId aid, String version) throws Exception;

	}

}
