/// <reference types="jasmine" />

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL, AUTH_TOKEN_KEY } from '../helpers/api-url';
import { LoginRequest, RegisterRequest, UserRole } from '../models/auth.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let authService: AuthService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    authService = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
    localStorage.removeItem(AUTH_TOKEN_KEY);
  });

  // Sends login credentials to the login endpoint
  it('posts login credentials', () => {
    const request: LoginRequest = { email: 'alice@example.com', password: 'password123' };
    authService.login(request).subscribe();

    const httpRequest = httpTestingController.expectOne(`${API_BASE_URL}/auth/login`);

    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush({ token: 'test-token', email: request.email, role: UserRole.Client });
  });

  // Sends registration details to the registration endpoint
  it('posts registration details', () => {
    const request: RegisterRequest = {
      name: 'Alice',
      email: 'alice@example.com',
      password: 'password123',
      role: UserRole.Client
    };
    authService.register(request).subscribe();

    const httpRequest = httpTestingController.expectOne(`${API_BASE_URL}/auth/register`);

    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush({ id: 'user-1', ...request });
  });

  // Saves the token and marks the user authenticated, then clears both on logout.
  it('saves and removes the authentication token', () => {
    authService.saveToken('test-token');

    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBe('test-token');
    expect(authService.isAuthenticated()).toBeTrue();

    authService.logout();

    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
    expect(authService.isAuthenticated()).toBeFalse();
  });

  // Identifies a seller from a valid JWT payload and rejects malformed tokens.
  it('identifies seller tokens and rejects malformed tokens', () => {
    const payload = btoa(JSON.stringify({ role: 'ROLE_SELLER' }));
    authService.saveToken(`header.${payload}.signature`);

    expect(authService.isSeller()).toBeTrue();

    authService.saveToken('not-a-jwt');

    expect(authService.isSeller()).toBeFalse();
  });
});