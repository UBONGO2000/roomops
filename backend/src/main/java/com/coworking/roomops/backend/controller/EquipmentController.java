package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.api.EquipmentsApi;
import com.coworking.roomops.backend.domain.Equipment;
import com.coworking.roomops.backend.domain.EquipmentStatut;
import com.coworking.roomops.backend.model.EquipmentResponse;
import com.coworking.roomops.backend.model.UpdateEquipmentStatusRequest;
import com.coworking.roomops.backend.service.EquipmentService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EquipmentController implements EquipmentsApi {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @Override
    public ResponseEntity<List<EquipmentResponse>> listEquipments() {
        return ResponseEntity.ok(equipmentService.listEquipments().stream().map(this::toResponse).toList());
    }

    @Override
    public ResponseEntity<EquipmentResponse> updateEquipmentStatus(
            Long equipmentId, UpdateEquipmentStatusRequest updateEquipmentStatusRequest) {
        EquipmentStatut newStatus = EquipmentStatut.valueOf(updateEquipmentStatusRequest.getStatut().name());
        Equipment updated = equipmentService.updateStatus(equipmentId, newStatus);
        return ResponseEntity.ok(toResponse(updated));
    }

    private EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse()
                .id(equipment.getId())
                .type(equipment.getType())
                .roomId(equipment.getRoom().getId())
                .roomName(equipment.getRoom().getNom())
                .statut(com.coworking.roomops.backend.model.EquipmentStatut.valueOf(equipment.getStatut().name()));
    }
}
