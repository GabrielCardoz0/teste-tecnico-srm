CREATE TABLE settlement (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idempotency_key UUID NOT NULL,
    receivable_id BIGINT NOT NULL REFERENCES receivable (id),
    receivable_type VARCHAR(30) NOT NULL,
    face_value NUMERIC(19, 2) NOT NULL,
    term_months INTEGER NOT NULL,
    base_rate NUMERIC(19, 8) NOT NULL,
    spread NUMERIC(19, 8) NOT NULL,
    present_value_brl NUMERIC(19, 2) NOT NULL,
    discount_brl NUMERIC(19, 2) NOT NULL,
    payment_currency VARCHAR(3) NOT NULL REFERENCES currency (code),
    exchange_rate_id BIGINT REFERENCES exchange_rate (id),
    exchange_rate NUMERIC(19, 8),
    payment_amount NUMERIC(19, 2) NOT NULL,
    settled_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_settlement_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uq_settlement_receivable UNIQUE (receivable_id),
    CONSTRAINT ck_settlement_positive_values
        CHECK (face_value > 0 AND term_months > 0 AND present_value_brl > 0 AND payment_amount > 0),
    CONSTRAINT ck_settlement_non_negative_rates
        CHECK (base_rate >= 0 AND spread >= 0 AND discount_brl >= 0),
    CONSTRAINT ck_settlement_rate_presence CHECK (
        (payment_currency = 'BRL' AND exchange_rate_id IS NULL AND exchange_rate IS NULL)
        OR (payment_currency <> 'BRL' AND exchange_rate_id IS NOT NULL AND exchange_rate IS NOT NULL)
    )
);
