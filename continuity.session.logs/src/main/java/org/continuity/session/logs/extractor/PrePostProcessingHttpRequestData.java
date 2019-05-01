package org.continuity.session.logs.extractor;

import java.util.Map;
import java.util.Optional;

/**
 * Represents a placeholder for pre and post processing of a modularized request.
 *
 * @author Henning Schulz
 *
 */
public class PrePostProcessingHttpRequestData implements HTTPRequestData {

	private final HTTPRequestData root;

	private final Mode mode;

	private final String businessTransaction;

	public PrePostProcessingHttpRequestData(HTTPRequestData root, Mode mode, String businessTransaction) {
		this.root = root;
		this.mode = mode;
		this.businessTransaction = businessTransaction;
	}

	@Override
	public String getIdentifier() {
		return mode.getIdentifier(root);
	}

	@Override
	public long getTimestamp() {
		return mode.getTimestamp(root);
	}

	@Override
	public long getResponseTime() {
		return 0;
	}

	@Override
	public String getUri() {
		return root.getUri();
	}

	@Override
	public Optional<Map<String, String[]>> getHTTPParameters() {
		return Optional.empty();
	}

	@Override
	public Optional<String> getRequestBody() {
		return Optional.empty();
	}

	@Override
	public int getPort() {
		return root.getPort();
	}

	@Override
	public String getHost() {
		return root.getHost();
	}

	@Override
	public String getRequestMethod() {
		return root.getRequestMethod();
	}

	@Override
	public long getResponseCode() {
		return root.getResponseCode();
	}

	@Override
	public String getSessionId() {
		return root.getSessionId();
	}

	@Override
	public String getBusinessTransaction() {
		String bt = businessTransaction;

		if (bt == null) {
			bt = root.getBusinessTransaction();
		}

		return mode.getPrefix() + bt;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	public static enum Mode {
		PRE("PRE_PROCESSING#") {
			@Override
			protected long getTimestamp(HTTPRequestData root) {
				return root.getTimestamp();
			}

			@Override
			protected String getIdentifier(HTTPRequestData root) {
				return root.getIdentifier() + "-PRE";
			}
		},
		POST("POST_PROCESSING#") {
			@Override
			protected long getTimestamp(HTTPRequestData root) {
				return root.getTimestamp() + root.getResponseTime();
			}

			@Override
			protected String getIdentifier(HTTPRequestData root) {
				return root.getIdentifier() + "-POST";
			}
		};

		private final String prefix;

		private Mode(String prefix) {
			this.prefix = prefix;
		}

		public String getPrefix() {
			return prefix;
		}

		protected abstract long getTimestamp(HTTPRequestData root);

		protected abstract String getIdentifier(HTTPRequestData root);

	}

}
