package com.srm.creditengine.domain;

public abstract class BusinessRuleViolationException extends RuntimeException {

	protected BusinessRuleViolationException(String message) {
		super(message);
	}

}
