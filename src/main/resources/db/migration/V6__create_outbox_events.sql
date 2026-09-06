CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY,
                               aggregate_type VARCHAR(50) NOT NULL,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(50) NOT NULL,
                               payload TEXT NOT NULL,
                               published BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL,
                               published_at TIMESTAMP
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(published, created_at) WHERE published = FALSE;