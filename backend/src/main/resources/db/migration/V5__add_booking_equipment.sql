-- Table de jonction reliant une réservation aux équipements de la salle qu'elle sollicite
-- réellement (choix éco-responsable, cf. api-contract/openapi.yaml BookingRequest.equipmentIds) :
-- un équipement en panne ne bloque la réservation que s'il figure dans cette liste
-- (BookingService.ensureRoomBookable), au lieu de bloquer sur toute panne de la salle.
CREATE TABLE booking_equipment (
    id           BIGSERIAL PRIMARY KEY,
    booking_id   BIGINT NOT NULL REFERENCES booking (id),
    equipment_id BIGINT NOT NULL REFERENCES equipment (id),
    CONSTRAINT uq_booking_equipment UNIQUE (booking_id, equipment_id)
);

CREATE INDEX idx_booking_equipment_booking_id ON booking_equipment (booking_id);
CREATE INDEX idx_booking_equipment_equipment_id ON booking_equipment (equipment_id);
