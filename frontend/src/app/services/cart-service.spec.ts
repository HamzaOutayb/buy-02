import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { CartItem, CartService } from './cart-service';

describe('CartService', () => {
  let service: CartService;
  let httpTesting: HttpTestingController;
  const flushSave = () => httpTesting.expectOne(`${environment.apiBaseUrl}/cart`).flush([]);

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('auth_token', 'test-token');
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CartService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('should start with empty cart', () => {
    expect(service.items().length).toBe(0);
    expect(service.count()).toBe(0);
    expect(service.subtotal()).toBe(0);
  });

  it('should add item and calculate totals', () => {
    const item: CartItem = {
      productId: 'p1',
      productName: 'Item 1',
      price: 25.5,
      quantity: 2,
      imageUrl: 'img.jpg',
      sellerId: 's1',
      maxQuantity: 10,
    };

    service.addToCart(item);
    flushSave();

    expect(service.items().length).toBe(1);
    expect(service.count()).toBe(2);
    expect(service.subtotal()).toBe(51.0);
  });

  it('should increment quantity when adding duplicate product up to maxQuantity', () => {
    const item: CartItem = {
      productId: 'p1',
      productName: 'Item 1',
      price: 10.0,
      quantity: 3,
      imageUrl: '',
      sellerId: 's1',
      maxQuantity: 5,
    };

    service.addToCart(item);
    flushSave();
    expect(service.count()).toBe(3);

    // Add 4 more, but capped at maxQuantity 5
    service.addToCart({ ...item, quantity: 4 });
    flushSave();
    expect(service.count()).toBe(5);
    expect(service.items()[0].quantity).toBe(5);
  });

  it('should update quantity within 1 and maxQuantity bounds', () => {
    const item: CartItem = {
      productId: 'p1',
      productName: 'Item 1',
      price: 10.0,
      quantity: 2,
      imageUrl: '',
      sellerId: 's1',
      maxQuantity: 5,
    };

    service.addToCart(item);
    flushSave();

    service.updateQuantity('p1', 4);
    flushSave();
    expect(service.items()[0].quantity).toBe(4);

    service.updateQuantity('p1', 10); // exceed max
    flushSave();
    expect(service.items()[0].quantity).toBe(5);

    service.updateQuantity('p1', 0); // below min
    flushSave();
    expect(service.items()[0].quantity).toBe(1);
  });

  it('should remove item from cart', () => {
    service.addToCart({
      productId: 'p1',
      productName: 'Item 1',
      price: 10.0,
      quantity: 1,
      imageUrl: '',
      sellerId: 's1',
      maxQuantity: 5,
    });
    flushSave();
    service.addToCart({
      productId: 'p2',
      productName: 'Item 2',
      price: 20.0,
      quantity: 1,
      imageUrl: '',
      sellerId: 's1',
      maxQuantity: 5,
    });
    flushSave();

    expect(service.items().length).toBe(2);

    service.removeFromCart('p1');
    flushSave();
    expect(service.items().length).toBe(1);
    expect(service.items()[0].productId).toBe('p2');
  });

  it('should clear cart', () => {
    service.addToCart({
      productId: 'p1',
      productName: 'Item 1',
      price: 10.0,
      quantity: 1,
      imageUrl: '',
      sellerId: 's1',
      maxQuantity: 5,
    });
    flushSave();

    service.clearCart();
    flushSave();
    expect(service.items().length).toBe(0);
    expect(service.count()).toBe(0);
    expect(service.subtotal()).toBe(0);
  });

  it('loads the signed-in user cart from the API instead of browser storage', () => {
    const item: CartItem = {
      productId: 'p1',
      productName: 'Database item',
      price: 20,
      quantity: 2,
      imageUrl: '',
      sellerId: 'seller-1',
      maxQuantity: 8,
    };
    localStorage.setItem('cart_items', JSON.stringify([{ ...item, productName: 'Old cart' }]));
    localStorage.setItem('auth_token', 'user-token');
    let completed = false;

    service.loadForUser().subscribe(() => (completed = true));

    const req = httpTesting.expectOne(`${environment.apiBaseUrl}/cart`);
    expect(req.request.method).toBe('GET');
    req.flush([item]);

    expect(completed).toBe(true);
    expect(service.items()).toEqual([item]);
    expect(service.items()[0].productName).toBe('Database item');
  });

  it('does not make cart contents available to a signed-out user', () => {
    localStorage.removeItem('auth_token');
    service.addToCart({
      productId: 'p1',
      productName: 'Item 1',
      price: 20,
      quantity: 1,
      imageUrl: '',
      sellerId: 'seller-1',
      maxQuantity: 8,
    });

    expect(service.items()).toEqual([]);
    httpTesting.expectNone(`${environment.apiBaseUrl}/cart`);
  });

  it('does not expose a stale cart while loading fails or the user signs out', () => {
    const staleItem: CartItem = {
      productId: 'stale',
      productName: 'Old account',
      price: 20,
      quantity: 1,
      imageUrl: '',
      sellerId: 'seller-1',
      maxQuantity: 8,
    };
    service.addToCart(staleItem);
    flushSave();
    service.addToCart(staleItem);
    flushSave();

    service.loadForUser().subscribe({ error: () => undefined });
    expect(service.items()).toEqual([]);
    httpTesting.expectOne(`${environment.apiBaseUrl}/cart`).flush(
      { message: 'Unavailable' },
      { status: 500, statusText: 'Server Error' },
    );
    expect(service.items()).toEqual([]);

    localStorage.setItem('cart_items', JSON.stringify([staleItem]));
    service.resetForLogout();
    expect(localStorage.getItem('cart_items')).toBeNull();
    expect(service.items()).toEqual([]);
  });

  it('persists product IDs and quantities to the user cart API', () => {
    localStorage.setItem('auth_token', 'user-token');
    service.addToCart({
      productId: 'p1',
      productName: 'Item 1',
      price: 20,
      quantity: 2,
      imageUrl: '',
      sellerId: 'seller-1',
      maxQuantity: 8,
    });

    const req = httpTesting.expectOne(`${environment.apiBaseUrl}/cart`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ items: [{ productId: 'p1', quantity: 2 }] });
    req.flush([]);
  });
});
