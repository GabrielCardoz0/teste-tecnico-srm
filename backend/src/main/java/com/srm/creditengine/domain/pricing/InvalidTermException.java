package com.srm.creditengine.domain.pricing;

import java.time.LocalDate;

public class InvalidTermException extends RuntimeException {

	public InvalidTermException(LocalDate dueDate, LocalDate today) {
		super("Due date " + dueDate + " must be after today (" + today + ")");
	}

}
