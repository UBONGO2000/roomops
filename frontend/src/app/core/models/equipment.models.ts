export type EquipmentStatut = 'OPERATIONNEL' | 'EN_PANNE';

export interface EquipmentResponse {
  id: number;
  type: string;
  roomId: number;
  roomName: string;
  statut: EquipmentStatut;
}

// Réponse dédiée à PUT /equipments/{id}/status : reservationsAnnulees n'a de sens que pour
// cette opération, pas pour un élément de la liste GET /equipments (cf. api-contract/openapi.yaml).
export interface EquipmentStatusUpdateResponse extends EquipmentResponse {
  reservationsAnnulees: number;
}
