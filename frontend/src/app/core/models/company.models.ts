export interface CompanyRequest {
  nom: string;
  siret?: string;
  adresseFacturation?: string;
  tarifHoraire?: number;
}

export interface CompanyResponse {
  id: number;
  nom: string;
  siret?: string;
  adresseFacturation?: string;
  tarifHoraire?: number;
}
