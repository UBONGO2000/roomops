package com.coworking.roomops.backend.mapper;

import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.model.EquipmentResponse;

public final class EquipmentMapper {

    private EquipmentMapper() {}

    public static EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse()
                .id(equipment.getId())
                .type(equipment.getType())
                .roomId(equipment.getRoom().getId())
                .roomName(equipment.getRoom().getNom())
                .statut(com.coworking.roomops.backend.model.EquipmentStatut.valueOf(equipment.getStatut().name()));
    }
}
