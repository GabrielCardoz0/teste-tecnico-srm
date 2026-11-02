package com.srm.creditengine.domain.receivable;

public class ReceivableNotFoundException extends RuntimeException {

	public ReceivableNotFoundException(Long id) {
		super("Receivable " + id + " not found");
	}

}
