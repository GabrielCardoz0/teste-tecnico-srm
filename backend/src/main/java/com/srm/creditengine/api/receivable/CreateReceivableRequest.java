package com.srm.creditengine.api.receivable;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.srm.creditengine.domain.pricing.ReceivableType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReceivableRequest(
		@NotNull Long assignorId,
		@NotNull ReceivableType type,
		@NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal faceValue,
		@NotNull LocalDate dueDate) {
}
