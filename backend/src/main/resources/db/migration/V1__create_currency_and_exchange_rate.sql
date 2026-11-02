CREATE TABLE currency (
    code VARCHAR(3) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT ck_currency_code_format CHECK (code ~ '^[A-Z]{3}$')
);

INSERT INTO currency (code, name) VALUES
    ('BRL', 'Brazilian Real'),
    ('USD', 'US Dollar');

CREATE TABLE exchange_rate (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_currency VARCHAR(3) NOT NULL REFERENCES currency (code),
    target_currency VARCHAR(3) NOT NULL REFERENCES currency (code),
    rate NUMERIC(19, 8) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_exchange_rate_distinct_currencies CHECK (source_currency <> target_currency),
    CONSTRAINT ck_exchange_rate_positive_rate CHECK (rate > 0)
);

CREATE INDEX ix_exchange_rate_current
    ON exchange_rate (source_currency, target_currency, valid_from DESC, id DESC);
