package com.srm.creditengine.domain.settlement;

public class SettlementNotFoundException extends RuntimeException {

	public SettlementNotFoundException(Long id) {
		super("Settlement " + id + " not found");
	}

}
