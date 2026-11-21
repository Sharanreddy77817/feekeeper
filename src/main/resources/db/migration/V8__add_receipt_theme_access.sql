ALTER TABLE tenants ADD COLUMN unlocked_themes varchar(150) NOT NULL DEFAULT 'ANDROMEDA';

UPDATE tenants SET unlocked_themes = CASE
    WHEN receipt_theme IS NOT NULL AND receipt_theme != 'ANDROMEDA' THEN 'ANDROMEDA,' || receipt_theme
    ELSE 'ANDROMEDA'
END;
