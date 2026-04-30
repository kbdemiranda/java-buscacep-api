CREATE TABLE cep_query_logs (
    id BIGSERIAL NOT NULL,
    external_id UUID NOT NULL,
    cep VARCHAR(8) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    request_timestamp TIMESTAMP NOT NULL,
    response_body JSONB,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_cep_query_logs PRIMARY KEY (id),
    CONSTRAINT uk_cep_query_logs_external_id UNIQUE (external_id)
);

CREATE INDEX idx_cep_query_logs_cep
    ON cep_query_logs (cep);

CREATE INDEX idx_cep_query_logs_request_timestamp
    ON cep_query_logs (request_timestamp);

CREATE INDEX idx_cep_query_logs_status
    ON cep_query_logs (status);
