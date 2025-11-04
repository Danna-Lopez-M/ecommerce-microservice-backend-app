package com.selimhorri.app.exception.wrapper;

public class ShippingNotFoundException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public ShippingNotFoundException() {
		super();
	}
	
	public ShippingNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ShippingNotFoundException(String message) {
		super(message);
	}
	
	public ShippingNotFoundException(Throwable cause) {
		super(cause);
	}
	
}
