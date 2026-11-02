package com.srm.creditengine.domain.assignor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.srm.creditengine.infrastructure.persistence.assignor.Assignor;
import com.srm.creditengine.infrastructure.persistence.assignor.AssignorRepository;

class AssignorServiceTest {

	private final AssignorRepository repository = mock(AssignorRepository.class);

	private final AssignorService service = new AssignorService(repository);

	@Test
	void activeAssignorIsReturned() {
		Assignor assignor = assignor(true);
		when(repository.findById(1L)).thenReturn(Optional.of(assignor));

		assertThat(service.requireActive(1L)).isSameAs(assignor);
	}

	@Test
	void unknownAssignorIsRejected() {
		when(repository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.requireActive(1L)).isInstanceOf(AssignorNotFoundException.class);
	}

	@Test
	void inactiveAssignorIsRejected() {
		Assignor assignor = assignor(false);
		when(repository.findById(1L)).thenReturn(Optional.of(assignor));

		assertThatThrownBy(() -> service.requireActive(1L)).isInstanceOf(InactiveAssignorException.class);
	}

	private static Assignor assignor(boolean active) {
		Assignor assignor = mock(Assignor.class);
		when(assignor.isActive()).thenReturn(active);
		return assignor;
	}

}
