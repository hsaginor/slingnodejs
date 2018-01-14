package org.apache.sling.scripting.nodejs.impl.engine;

public class V8WrapperCallException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5971761907729984462L;

	public V8WrapperCallException() {
		
	}

	public V8WrapperCallException(String message) {
		super(message);
	}

	public V8WrapperCallException(Throwable cause) {
		super(cause);
	}

	public V8WrapperCallException(String message, Throwable cause) {
		super(message, cause);
	}

	public V8WrapperCallException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
