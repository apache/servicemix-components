package org.servicemix.wsn.jms;

public class InvalidTopicException extends Exception {

	private static final long serialVersionUID = -3708397351142080702L;

	public InvalidTopicException() {
		super();
	}

	public InvalidTopicException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidTopicException(String message) {
		super(message);
	}

	public InvalidTopicException(Throwable cause) {
		super(cause);
	}

}
