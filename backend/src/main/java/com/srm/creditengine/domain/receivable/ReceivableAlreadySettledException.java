package com.srm.creditengine.domain.receivable;

public class ReceivableAlreadySettledException extends RuntimeException {

	public ReceivableAlreadySettledException(Long id) {
		super("Receivable " + id + " is already settled");
	}

}
