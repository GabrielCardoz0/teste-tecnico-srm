package com.srm.creditengine.infrastructure.persistence.receivable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.receivable.ReceivableAlreadySettledException;
import com.srm.creditengine.domain.receivable.ReceivableStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "receivable")
public class Receivable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "assignor_id", nullable = false)
	private Long assignorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private ReceivableType type;

	@Column(name = "face_value", nullable = false, precision = 19, scale = 2)
	private BigDecimal faceValue;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ReceivableStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Receivable() {
	}

	public Receivable(Long assignorId, ReceivableType type, BigDecimal faceValue, LocalDate dueDate,
			ReceivableStatus status, Instant createdAt) {
		this.assignorId = assignorId;
		this.type = type;
		this.faceValue = faceValue;
		this.dueDate = dueDate;
		this.status = status;
		this.createdAt = createdAt;
	}

	public void markSettled() {
		if (status != ReceivableStatus.PENDING) {
			throw new ReceivableAlreadySettledException(id);
		}
		status = ReceivableStatus.SETTLED;
	}

	public Long getId() {
		return id;
	}

	public Long getAssignorId() {
		return assignorId;
	}

	public ReceivableType getType() {
		return type;
	}

	public BigDecimal getFaceValue() {
		return faceValue;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public ReceivableStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
