package com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

/**
 * Monthly spread for one receivable type, as a decimal fraction (1.5% = 0.015).
 */
public interface SpreadStrategy {

	ReceivableType type();

	BigDecimal spread();

}
