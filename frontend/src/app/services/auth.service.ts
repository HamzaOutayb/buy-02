import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL, AUTH_TOKEN_KEY } from '../helpers/api-url';
import {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse
} from '../models/auth.model';
import { UserService } from './user.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly userService = inject(UserService);
  private readonly authenticatedState = signal(
    Boolean(localStorage.getItem(AUTH_TOKEN_KEY))
  );

  readonly authenticated = this.authenticatedState.asReadonly();

  login(request: LoginRequest): Observable<LoginResponse>
  {
    return this.http.post<LoginResponse>(
      `${API_BASE_URL}/auth/login`,
      request
    );
  }

  register(request: RegisterRequest): Observable<RegisterResponse>
  {
    return this.http.post<RegisterResponse>(
      `${API_BASE_URL}/auth/register`,
      request
    );
  }

  saveToken(token: string): void
  {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
    this.authenticatedState.set(true);
  }

  logout(): void
  {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    this.authenticatedState.set(false);
  }

  isAuthenticated(): boolean
  {
    return this.authenticatedState();
  }

  isSeller(): boolean
  {
    const role = this.userService.currentUser()?.role;
    return role === 'ROLE_SELLER' || role === 'SELLER';
  }
}
