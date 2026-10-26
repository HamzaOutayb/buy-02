/// <reference types="jasmine" />

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL, AUTH_TOKEN_KEY } from '../helpers/api-url';
import { LoginRequest, RegisterRequest, UserRole } from '../models/auth.model';
import { UserService } from './user.service';
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

  it('identifies sellers from the loaded current user and rejects cleared users', () => {
    const userService = TestBed.inject(UserService);
    userService.getCurrentUser().subscribe();
    const request = httpTestingController.expectOne(`${API_BASE_URL}/me`);
    request.flush({ id: 'seller-1', name: 'Seller', email: 'seller@example.com', role: 'ROLE_SELLER', avatar: null });

    expect(authService.isSeller()).toBeTrue();

    userService.clearCurrentUser();
    expect(authService.isSeller()).toBeFalse();
  });
});
