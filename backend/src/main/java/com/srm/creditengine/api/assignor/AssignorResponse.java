package com.srm.creditengine.api.assignor;

import com.srm.creditengine.infrastructure.persistence.assignor.Assignor;

public record AssignorResponse(Long id, String cnpj, String legalName) {

	static AssignorResponse from(Assignor assignor) {
		return new AssignorResponse(assignor.getId(), assignor.getCnpj(), assignor.getLegalName());
	}

}
