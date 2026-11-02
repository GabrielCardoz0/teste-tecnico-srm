package com.srm.creditengine.api.settlement;

import com.srm.creditengine.domain.pricing.PaymentCurrency;

import jakarta.validation.constraints.NotNull;

public record SettleRequest(@NotNull Long receivableId, @NotNull PaymentCurrency paymentCurrency) {
}
