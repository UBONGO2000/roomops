-- Production-only cleanup of the historical demonstration seed data.
-- Flyway substitutes the placeholder; in development the block intentionally does nothing.
DO $$
BEGIN
    IF '${production}' = 'true' THEN
        DELETE FROM users
        WHERE email IN (
            'admin@roomops.local',
            'manager.nova@roomops.local',
            'employe1.nova@roomops.local',
            'employe2.nova@roomops.local',
            'manager.kaizen@roomops.local',
            'employe1.kaizen@roomops.local',
            'employe2.kaizen@roomops.local'
        );

        DELETE FROM company
        WHERE nom IN ('Nova Digital Studio', 'Atelier Kaizen Conseil');
    END IF;
END $$;