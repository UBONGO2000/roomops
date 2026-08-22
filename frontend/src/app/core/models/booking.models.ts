export type BookingStatut = 'CONFIRMEE' | 'ANNULEE';

export interface BookingRequest {
  roomId: number;
  dateDebut: string;
  dateFin: string;
  motif?: string;
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
