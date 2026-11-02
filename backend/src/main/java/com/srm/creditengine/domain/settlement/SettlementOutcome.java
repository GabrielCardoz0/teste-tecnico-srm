package com.srm.creditengine.domain.settlement;

import com.srm.creditengine.infrastructure.persistence.settlement.Settlement;

public record SettlementOutcome(Settlement settlement, boolean created) {

	static SettlementOutcome created(Settlement settlement) {
		return new SettlementOutcome(settlement, true);
	}

	static SettlementOutcome replayed(Settlement settlement) {
		return new SettlementOutcome(settlement, false);
	}

}
