BEGIN;

ALTER TABLE banners
    ADD COLUMN IF NOT EXISTS placement VARCHAR(20) NOT NULL DEFAULT 'BANNER';

ALTER TABLE banners
    DROP CONSTRAINT IF EXISTS chk_banners_placement;

ALTER TABLE banners
    ADD CONSTRAINT chk_banners_placement
        CHECK (placement IN ('SPLASH', 'BANNER', 'BOTH'));

CREATE INDEX IF NOT EXISTS idx_banners_active_placement_order
    ON banners (is_active, placement, display_order);

COMMIT;
