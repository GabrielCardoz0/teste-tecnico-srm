package com.srm.creditengine.infrastructure.persistence.settlement;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

public interface SettlementRepository extends Repository<Settlement, Long> {

	Settlement save(Settlement settlement);

	Optional<Settlement> findById(Long id);

	Optional<Settlement> findByIdempotencyKey(UUID idempotencyKey);

}
