export interface RoomResponse {
  id: number;
  nom: string;
  capacite: number;
  buildingId: number;
  buildingName: string;
  estActif: boolean;
  indisponibilite?: string | null;
}

export interface AvailabilityResponse {
  roomId: number;
  roomName: string;
  isAvailable: boolean;
  reason?: string | null;
  dateDebut: string;
  dateFin: string;
}
