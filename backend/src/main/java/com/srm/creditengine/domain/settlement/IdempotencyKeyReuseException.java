package com.srm.creditengine.domain.settlement;

import java.util.UUID;

import com.srm.creditengine.domain.BusinessRuleViolationException;

public class IdempotencyKeyReuseException extends BusinessRuleViolationException {

	public IdempotencyKeyReuseException(UUID key) {
		super("Idempotency key " + key + " was already used for a different settlement request");
	}

}
