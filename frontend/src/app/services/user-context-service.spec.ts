import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CartService } from './cart-service';
import { UserContextService, UserProfile } from './user-context-service';

describe('UserContextService', () => {
  let service: UserContextService;
  let httpTesting: HttpTestingController;
  const profile: UserProfile = {
    id: 'user-1',
    name: 'Customer',
    email: 'customer@example.com',
    avatar: null,
    role: 'CLIENT',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserContextService);
    httpTesting = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => httpTesting.verify());

  it('skips loading from the API when no token is stored', () => {
    let completed = false;
    service.load().subscribe(() => (completed = true));

    expect(completed).toBe(true);
    expect(service.user()).toBeNull();
    expect(service.isLoading()).toBe(false);
    httpTesting.expectNone('https://localhost:8080/api/v1/users/me');
  });

  it('loads and stores the current profile when a token exists', () => {
    localStorage.setItem('auth_token', 'Bearer token');
    let completed = false;

    service.load().subscribe(() => (completed = true));

    expect(service.isLoading()).toBe(true);
    const req = httpTesting.expectOne('https://localhost:8080/api/v1/users/me');
    expect(req.request.method).toBe('GET');
    req.flush(profile);
    httpTesting.expectOne('https://localhost:8080/api/v1/cart').flush([]);

    expect(completed).toBe(true);
    expect(service.user()).toEqual(profile);
    expect(service.isLoading()).toBe(false);
    expect(localStorage.getItem('auth_token')).toBe('Bearer token');
  });

  it('shares profile lookup and does not request a cart for sellers', () => {
    const seller: UserProfile = { ...profile, role: 'SELLER' };
    localStorage.setItem('auth_token', 'seller-token');

    service.load().subscribe();
    service.load().subscribe();

    const req = httpTesting.expectOne('https://localhost:8080/api/v1/users/me');
    req.flush(seller);

    expect(service.user()).toEqual(seller);
    httpTesting.expectNone('https://localhost:8080/api/v1/cart');
  });

  it('clears the token and profile when loading the profile fails', () => {
    localStorage.setItem('auth_token', 'Bearer expired');
    service.load().subscribe();
    const req = httpTesting.expectOne('https://localhost:8080/api/v1/users/me');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(service.user()).toBeNull();
    expect(service.isLoading()).toBe(false);
  });

  it('updates and stores the profile after a successful update', () => {
    const request = {
      name: 'Updated customer',
      email: 'updated@example.com',
      avatar: 'avatar.png',
      password: 'new-secret',
    };
    const updated: UserProfile = {
      ...profile,
      name: request.name,
      email: request.email,
      avatar: request.avatar,
    };
    service.updateProfile(request).subscribe((actual) => expect(actual).toEqual(updated));
    const req = httpTesting.expectOne('https://localhost:8080/api/v1/users/me');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(updated);

    expect(service.user()).toEqual(updated);
  });

  it('clears the token and user when logging out', () => {
    localStorage.setItem('auth_token', 'test-token');
    const cartService = TestBed.inject(CartService);
    cartService.addToCart({
      productId: 'seller-product',
      productName: 'Seller product',
      price: 25,
      quantity: 1,
      imageUrl: '',
      sellerId: 'seller-1',
      maxQuantity: 5,
    });
    httpTesting.expectOne('https://localhost:8080/api/v1/cart').flush([]);
    localStorage.setItem('auth_token', 'Bearer token');
    service.updateProfile({
      name: profile.name,
      email: profile.email,
      avatar: profile.avatar,
      password: 'secret',
    }).subscribe();
    httpTesting.expectOne('https://localhost:8080/api/v1/users/me').flush(profile);

    service.logout();

    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(service.user()).toBeNull();
    expect(cartService.items()).toEqual([]);
    expect(localStorage.getItem('cart_items')).toBeNull();
  });
});
