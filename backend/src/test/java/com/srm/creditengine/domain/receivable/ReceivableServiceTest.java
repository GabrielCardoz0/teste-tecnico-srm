package com.srm.creditengine.domain.receivable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.srm.creditengine.domain.assignor.AssignorService;
import com.srm.creditengine.domain.assignor.InactiveAssignorException;
import com.srm.creditengine.domain.pricing.InvalidTermException;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.pricing.TermCalculator;
import com.srm.creditengine.infrastructure.persistence.receivable.Receivable;
import com.srm.creditengine.infrastructure.persistence.receivable.ReceivableRepository;

class ReceivableServiceTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T15:00:00.123456789Z"), SAO_PAULO);

	private final ReceivableRepository repository = mock(ReceivableRepository.class);

	private final AssignorService assignorService = mock(AssignorService.class);

	private final ReceivableService service = new ReceivableService(repository, assignorService,
			new TermCalculator(CLOCK), CLOCK);

	@Test
	void registersPendingReceivableMatchingDatabasePrecision() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Receivable saved = service.register(1L, ReceivableType.DUPLICATA_MERCANTIL, new BigDecimal("100000"),
				LocalDate.of(2026, 4, 15));

		assertThat(saved.getStatus()).isEqualTo(ReceivableStatus.PENDING);
		assertThat(saved.getFaceValue()).isEqualTo(new BigDecimal("100000.00"));
		assertThat(saved.getCreatedAt()).isEqualTo(Instant.parse("2026-01-15T15:00:00.123456Z"));
		verify(assignorService).requireActive(1L);
	}

	@Test
	void rejectedAssignorIsNotSaved() {
		when(assignorService.requireActive(1L)).thenThrow(new InactiveAssignorException(1L));

		assertThatThrownBy(() -> service.register(1L, ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100.00"), LocalDate.of(2026, 4, 15)))
				.isInstanceOf(InactiveAssignorException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void dueDateTodayIsRejectedAndNotSaved() {
		assertThatThrownBy(() -> service.register(1L, ReceivableType.DUPLICATA_MERCANTIL,
				new BigDecimal("100.00"), LocalDate.of(2026, 1, 15)))
				.isInstanceOf(InvalidTermException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(repository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findById(42L)).isInstanceOf(ReceivableNotFoundException.class);
	}

}
