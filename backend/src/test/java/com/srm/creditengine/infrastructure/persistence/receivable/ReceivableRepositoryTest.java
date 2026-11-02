package com.srm.creditengine.infrastructure.persistence.receivable;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.srm.creditengine.TestcontainersConfiguration;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.receivable.ReceivableStatus;
import com.srm.creditengine.infrastructure.persistence.assignor.Assignor;
import com.srm.creditengine.infrastructure.persistence.assignor.AssignorRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ReceivableRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-01-15T15:00:00Z");

	private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20, Sort.by("id"));

	@Autowired
	ReceivableRepository repository;

	@Autowired
	AssignorRepository assignorRepository;

	private Long alfaId;
	private Long betaId;

	@BeforeEach
	void seedReceivables() {
		List<Assignor> active = assignorRepository.findAllByActiveTrueOrderByLegalNameAsc();
		alfaId = active.get(0).getId();
		betaId = active.get(1).getId();

		save(alfaId, "100000.00", LocalDate.of(2026, 4, 15), ReceivableStatus.PENDING);
		save(alfaId, "25000.00", LocalDate.of(2026, 3, 15), ReceivableStatus.SETTLED);
		save(betaId, "100000.00", LocalDate.of(2026, 4, 15), ReceivableStatus.PENDING);
	}

	@Test
	void withoutFiltersReturnsEverything() {
		assertThat(search(null, null, null, null).getTotalElements()).isEqualTo(3);
	}

	@Test
	void filtersByStatus() {
		Page<Receivable> settled = search(ReceivableStatus.SETTLED, null, null, null);

		assertThat(settled.getContent()).extracting(Receivable::getFaceValue)
				.containsExactly(new BigDecimal("25000.00"));
	}

	@Test
	void filtersByAssignor() {
		assertThat(search(null, betaId, null, null).getContent()).extracting(Receivable::getAssignorId)
				.containsExactly(betaId);
	}

	@Test
	void filtersByFaceValueAndDueDateForDuplicateCheck() {
		Page<Receivable> duplicates = search(null, alfaId, new BigDecimal("100000.00"), LocalDate.of(2026, 4, 15));

		assertThat(duplicates.getTotalElements()).isEqualTo(1);
	}

	@Test
	void pagesAreBoundedBySize() {
		Page<Receivable> firstPage = repository.search(null, null, null, null, PageRequest.of(0, 2, Sort.by("id")));

		assertThat(firstPage.getContent()).hasSize(2);
		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);
	}

	private Page<Receivable> search(ReceivableStatus status, Long assignorId, BigDecimal faceValue,
			LocalDate dueDate) {
		return repository.search(status, assignorId, faceValue, dueDate, FIRST_PAGE);
	}

	private void save(Long assignorId, String faceValue, LocalDate dueDate, ReceivableStatus status) {
		repository.save(new Receivable(assignorId, ReceivableType.DUPLICATA_MERCANTIL, new BigDecimal(faceValue),
				dueDate, status, NOW));
	}

}
