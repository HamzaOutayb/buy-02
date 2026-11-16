import { TestBed } from '@angular/core/testing';
import { CartItem, CartService } from './cart-service';

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  });

  afterEach(() => {
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
    expect(service.count()).toBe(3);

    // Add 4 more, but capped at maxQuantity 5
    service.addToCart({ ...item, quantity: 4 });
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

    service.updateQuantity('p1', 4);
    expect(service.items()[0].quantity).toBe(4);

    service.updateQuantity('p1', 10); // exceed max
    expect(service.items()[0].quantity).toBe(5);

    service.updateQuantity('p1', 0); // below min
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
    service.addToCart({
      productId: 'p2',
      productName: 'Item 2',
      price: 20.0,
      quantity: 1,
      imageUrl: '',
      sellerId: 's1',
      maxQuantity: 5,
    });

    expect(service.items().length).toBe(2);

    service.removeFromCart('p1');
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

    service.clearCart();
    expect(service.items().length).toBe(0);
    expect(service.count()).toBe(0);
    expect(service.subtotal()).toBe(0);
  });
});
