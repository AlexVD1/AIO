CREATE TABLE batch_job (
    id UUID PRIMARY KEY,
    total_requested INT NOT NULL,
    total_completed INT NOT NULL DEFAULT 0,
    total_failed INT NOT NULL DEFAULT 0,
    genre_distribution_json TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE INDEX idx_batch_job_status ON batch_job(status);
CREATE INDEX idx_batch_job_created_at ON batch_job(created_at);
