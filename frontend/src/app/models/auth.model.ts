export enum UserRole {
  Client = 'CLIENT',
  Seller = 'SELLER'
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  role: UserRole;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  role: UserRole;
}

export interface RegisterResponse {
  id: string;
  name: string;
  email: string;
  role: string;
}
