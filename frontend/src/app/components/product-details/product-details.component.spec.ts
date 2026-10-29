/// <reference types="jasmine" />

import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Product } from '../../models/product.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { ProductService } from '../../services/product.service';
import { ProductDetailsComponent } from './product-details.component';

/**
 * ProductDetailsComponent Unit Tests (Angular / Jasmine)
 *
 * Why Test ProductDetailsComponent?
 * - It demonstrates how to test a component that depends on:
 *   1. ActivatedRoute parameters (extracting :id from the URL).
 *   2. ProductService (fetching product details for that :id).
 *   3. Interactive UI logic (image carousel navigation).
 */
describe('ProductDetailsComponent', () => {
  let fixture: ComponentFixture<ProductDetailsComponent>;
  let component: ProductDetailsComponent;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const mockProduct: Product = {
    id: 'prod-123',
    name: 'Wireless Headphones',
    description: 'Noise cancelling over-ear headphones',
    price: 199.99,
    quantity: 12,
    sellerId: 'seller-1',
    imageUrls: [
      'https://example.com/headphones-front.jpg',
      'https://example.com/headphones-side.jpg',
      'https://example.com/headphones-case.jpg'
    ]
  };

  /**
   * Helper function to configure TestBed with an optional route param ID.
   */
  async function setupTestingModule(routeId: string | null) {
    productServiceSpy = jasmine.createSpyObj<ProductService>('ProductService', ['getProduct']);
    cartServiceSpy = jasmine.createSpyObj<CartService>('CartService', ['addItem']);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated', 'isClient']);

    await TestBed.configureTestingModule({
      imports: [ProductDetailsComponent],
      providers: [
        provideRouter([]),
        { provide: ProductService, useValue: productServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(routeId ? { id: routeId } : {})
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetailsComponent);
    component = fixture.componentInstance;
  }

  // =========================================================================
  // 1. Successful product loading via route parameter
  // =========================================================================
  it('should load product details when a valid id exists in the route', async () => {
    await setupTestingModule('prod-123');

    // ARRANGE: Mock service to return the product
    productServiceSpy.getProduct.and.returnValue(of(mockProduct));

    // ACT: Trigger ngOnInit()
    fixture.detectChanges();

    // ASSERT:
    expect(component.isLoading).toBeFalse();
    expect(component.errorMessage).toBe('');
    expect(component.product).toEqual(mockProduct);
    expect(component.imageUrls.length).toBe(3);
    expect(component.activeImageIndex).toBe(0);

    // Verify service was queried with the correct route param ID
    expect(productServiceSpy.getProduct).toHaveBeenCalledWith('prod-123');
  });

  // =========================================================================
  // 2. Error handling when product cannot be loaded
  // =========================================================================
  it('should display error message when product fetch fails with 404', async () => {
    await setupTestingModule('prod-999');

    // ARRANGE: Mock a 404 Not Found error response
    const errorResponse = new HttpErrorResponse({
      status: 404,
      statusText: 'Not Found',
      error: { message: 'Product not found' }
    });
    productServiceSpy.getProduct.and.returnValue(throwError(() => errorResponse));

    // ACT:
    fixture.detectChanges();

    // ASSERT:
    expect(component.isLoading).toBeFalse();
    expect(component.product).toBeNull();
    expect(component.errorMessage).toBeTruthy();
  });

  // =========================================================================
  // 3. Handling missing route parameter
  // =========================================================================
  it('should display not found message when route has no product id', async () => {
    // Configure route without an 'id' param
    await setupTestingModule(null);

    // ACT:
    fixture.detectChanges();

    // ASSERT:
    expect(component.isLoading).toBeFalse();
    expect(component.errorMessage).toBe('This product could not be found.');
    // ProductService.getProduct should not have been called
    expect(productServiceSpy.getProduct).not.toHaveBeenCalled();
  });

  // =========================================================================
  // 4. Image gallery carousel navigation
  // =========================================================================
  it('should navigate through images correctly with next and previous buttons', async () => {
    await setupTestingModule('prod-123');
    productServiceSpy.getProduct.and.returnValue(of(mockProduct));
    fixture.detectChanges();

    // Initial state: first image (index 0)
    expect(component.activeImageIndex).toBe(0);

    // 1. Next image: moves to index 1
    component.showNextImage();
    expect(component.activeImageIndex).toBe(1);

    // 2. Next image again: moves to index 2 (last image)
    component.showNextImage();
    expect(component.activeImageIndex).toBe(2);

    // 3. Next image from last image wraps around to index 0
    component.showNextImage();
    expect(component.activeImageIndex).toBe(0);

    // 4. Previous image from index 0 wraps around to the last image (index 2)
    component.showPreviousImage();
    expect(component.activeImageIndex).toBe(2);

    // 5. Direct selection: jump to index 1
    component.selectImage(1);
    expect(component.activeImageIndex).toBe(1);
  });

  it('should add the product to the buyer cart', async () => {
    await setupTestingModule('prod-123');
    productServiceSpy.getProduct.and.returnValue(of(mockProduct));
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.isClient.and.returnValue(true);
    cartServiceSpy.addItem.and.returnValue(of({ items: [], subtotal: 0 }));
    fixture.detectChanges();

    component.addToCart();

    expect(cartServiceSpy.addItem).toHaveBeenCalledWith('prod-123');
    expect(component.cartMessage).toBe('Added to your cart.');
  });

  it('does not add products to the cart for non-client accounts', async () => {
    await setupTestingModule('prod-123');
    productServiceSpy.getProduct.and.returnValue(of(mockProduct));
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.isClient.and.returnValue(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.add-to-cart-button')).toBeNull();
    component.addToCart();

    expect(cartServiceSpy.addItem).not.toHaveBeenCalled();
    expect(component.cartMessage).toBe('Only clients can add available products to a cart.');
  });
});
