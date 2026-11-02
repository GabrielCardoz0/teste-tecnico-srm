package com.srm.creditengine.api.receivable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.receivable.ReceivableStatus;
import com.srm.creditengine.infrastructure.persistence.receivable.Receivable;

public record ReceivableResponse(
		Long id,
		Long assignorId,
		ReceivableType type,
		BigDecimal faceValue,
		LocalDate dueDate,
		ReceivableStatus status,
		Instant createdAt) {

	static ReceivableResponse from(Receivable receivable) {
		return new ReceivableResponse(receivable.getId(), receivable.getAssignorId(), receivable.getType(),
				receivable.getFaceValue(), receivable.getDueDate(), receivable.getStatus(), receivable.getCreatedAt());
	}

}
