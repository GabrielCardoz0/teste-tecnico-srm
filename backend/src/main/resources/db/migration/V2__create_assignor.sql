CREATE TABLE assignor (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL,
    legal_name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_assignor_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_assignor_cnpj_format CHECK (cnpj ~ '^[A-Z0-9]{12}[0-9]{2}$')
);

INSERT INTO assignor (cnpj, legal_name, active) VALUES
    ('A1B2C3D4000193', 'Delta Serviços Logísticos Ltda', TRUE),
    ('B7C8D9E0000120', 'Gama Indústria Metalúrgica Ltda', FALSE),
    ('11222333000181', 'Alfa Comércio de Tecidos Ltda', TRUE),
    ('12ABC34501DE35', 'Beta Distribuidora de Alimentos S.A.', TRUE);
