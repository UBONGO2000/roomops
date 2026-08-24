-- Distingue une annulation volontaire (BookingService.cancelBooking) d'une annulation en
-- cascade suite à une panne d'équipement (EquipmentService.cancelFutureBookingsForRoom).
-- NULL pour les réservations non annulées, et pour celles déjà annulées avant cette migration
-- (raison inconnue, aucune reconstruction rétroactive possible).
ALTER TABLE booking
    ADD COLUMN raison_annulation VARCHAR(30)
        CHECK (raison_annulation IS NULL OR raison_annulation IN ('MANUELLE', 'PANNE_EQUIPEMENT'));
