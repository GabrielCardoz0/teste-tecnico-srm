package com.srm.creditengine.infrastructure.persistence.receivable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.srm.creditengine.domain.receivable.ReceivableStatus;

public interface ReceivableRepository extends Repository<Receivable, Long> {

	Receivable save(Receivable receivable);

	Optional<Receivable> findById(Long id);

	@Query("""
			SELECT r FROM Receivable r
			WHERE (:status IS NULL OR r.status = :status)
			  AND (:assignorId IS NULL OR r.assignorId = :assignorId)
			  AND (:faceValue IS NULL OR r.faceValue = :faceValue)
			  AND (CAST(:dueDate AS LocalDate) IS NULL OR r.dueDate = :dueDate)
			""")
	Page<Receivable> search(@Param("status") ReceivableStatus status, @Param("assignorId") Long assignorId,
			@Param("faceValue") BigDecimal faceValue, @Param("dueDate") LocalDate dueDate, Pageable pageable);

}
