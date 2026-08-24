import { EquipmentResponse } from './equipment.models';

export type BookingStatut = 'CONFIRMEE' | 'ANNULEE';

// Null si la réservation n'est pas annulée. MANUELLE : annulation volontaire (DELETE
// /bookings/{id}). PANNE_EQUIPEMENT : annulation en cascade suite à une panne d'équipement.
export type RaisonAnnulation = 'MANUELLE' | 'PANNE_EQUIPEMENT';

export interface BookingRequest {
  roomId: number;
  dateDebut: string;
  dateFin: string;
  motif?: string;
  equipmentIds?: number[];
}

export interface BookingResponse {
  id: number;
  roomId: number;
  roomName: string;
  userId: number;
  userName: string;
  companyName: string;
  dateDebut: string;
  dateFin: string;
  statut: BookingStatut;
  motif?: string;
  raisonAnnulation?: RaisonAnnulation | null;
  equipmentsActives?: EquipmentResponse[];
  ecoScore?: number;
  version: number;
}

export interface BookingPageResponse {
  content: BookingResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ListBookingsParams {
  roomId?: number;
  dateDebut?: string;
  dateFin?: string;
  statut?: BookingStatut;
  page?: number;
  size?: number;
}
