package com.selimhorri.app.exception.wrapper;

public class MissingUserDtoException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public MissingUserDtoException() {
		super();
	}
	
	public MissingUserDtoException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public MissingUserDtoException(String message) {
		super(message);
	}
	
	public MissingUserDtoException(Throwable cause) {
		super(cause);
	}
	
	
	
}

