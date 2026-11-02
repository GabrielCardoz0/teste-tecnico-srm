CREATE TABLE receivable (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    assignor_id BIGINT NOT NULL REFERENCES assignor (id),
    type VARCHAR(30) NOT NULL,
    face_value NUMERIC(19, 2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_receivable_positive_face_value CHECK (face_value > 0),
    CONSTRAINT ck_receivable_status CHECK (status IN ('PENDING', 'SETTLED'))
);

CREATE INDEX ix_receivable_status ON receivable (status);

CREATE INDEX ix_receivable_assignor_due_date_face_value ON receivable (assignor_id, due_date, face_value);
