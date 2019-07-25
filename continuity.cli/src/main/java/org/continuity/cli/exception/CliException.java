package org.continuity.cli.exception;

import org.continuity.cli.utils.ResponseBuilder;
import org.jline.utils.AttributedString;

public class CliException extends Exception {

	private static final long serialVersionUID = 6684010595987409683L;

	private final AttributedString message;

	public CliException(AttributedString message) {
		this.message = message;
	}

	public CliException(String message) {
		this.message = new ResponseBuilder().error(message).build();
	}

	@Override
	public String getMessage() {
		return message.toString();
	}

	public AttributedString getAttributedMessage() {
		return message;
	}

}
