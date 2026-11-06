BEGIN;

CREATE TABLE printers (
    id UUID PRIMARY KEY,
    building_id VARCHAR(100) NOT NULL REFERENCES campus_buildings(id),
    detail_location TEXT NOT NULL,
    name VARCHAR(100),
    price TEXT,
    payment_method TEXT,
    notes TEXT,
    broken BOOLEAN NOT NULL DEFAULT FALSE,
    removed BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (CHAR_LENGTH(TRIM(detail_location)) > 0)
);
CREATE INDEX idx_printers_building_visibility ON printers(building_id, deleted, removed);

CREATE TABLE printer_reports (
    id UUID PRIMARY KEY,
    type VARCHAR(20) NOT NULL CHECK (type IN ('NEW', 'UPDATE')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    printer_id UUID REFERENCES printers(id),
    reporter_device_id UUID NOT NULL,
    proposal TEXT NOT NULL,
    original_snapshot TEXT,
    applied_snapshot TEXT,
    original_version BIGINT,
    description VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    processed_by VARCHAR(255),
    rejection_reason VARCHAR(1000),
    CHECK (type <> 'UPDATE' OR (printer_id IS NOT NULL AND original_snapshot IS NOT NULL AND original_version IS NOT NULL)),
    CHECK ((status = 'PENDING' AND processed_at IS NULL AND processed_by IS NULL)
        OR (status <> 'PENDING' AND processed_at IS NOT NULL AND processed_by IS NOT NULL)),
    CHECK (status <> 'REJECTED' OR rejection_reason IS NOT NULL),
    CHECK (status <> 'APPROVED' OR (printer_id IS NOT NULL AND applied_snapshot IS NOT NULL))
);
CREATE INDEX idx_printer_reports_status_created ON printer_reports(status, created_at, id);
CREATE INDEX idx_printer_reports_printer ON printer_reports(printer_id);

COMMIT;
