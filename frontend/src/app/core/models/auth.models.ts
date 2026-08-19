export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  tokenType: string;
}

export type Role = 'SUPER_ADMIN' | 'MANAGER' | 'EMPLOYEE';

export interface UserResponse {
  id: number;
  email: string;
  nom: string;
  prenom: string;
  role: Role;
  companyId?: number | null;
  companyName?: string | null;
}
