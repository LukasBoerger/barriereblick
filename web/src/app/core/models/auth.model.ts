export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  token: string;
  expiresAt: string; // ISO 8601 from backend
}
