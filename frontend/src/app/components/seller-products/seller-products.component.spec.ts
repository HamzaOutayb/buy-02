/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Product } from '../../models/product.model';
import { UserProfile } from '../../models/user.model';
import { ProductService } from '../../services/product.service';
import { UserService } from '../../services/user.service';
import { SellerProductsComponent } from './seller-products.component';

/**
 * SellerProductsComponent Unit Tests (Angular / Jasmine)
 *
 * Why Test SellerProductsComponent?
 * - It demonstrates how to test a component that coordinates multiple services:
 *   1. UserService: to get current seller's ID.
 *   2. ProductService: to fetch all products and filter by sellerId.
 *   3. Product deletion: user confirmation dialog & deleting via ProductService.
 */
describe('SellerProductsComponent', () => {
  let fixture: ComponentFixture<SellerProductsComponent>;
  let component: SellerProductsComponent;
  let productServiceSpy: jasmine.SpyObj<ProductService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  const mockUser: UserProfile = {
    id: 'seller-123',
    name: 'Alice Seller',
    email: 'alice@example.com',
    role: 'ROLE_SELLER',
    avatar: null
  };

  const sellerProduct: Product = {
    id: 'prod-1',
    name: 'Handmade Mug',
    description: 'Ceramic mug',
    price: 15.00,
    quantity: 10,
    sellerId: 'seller-123', // Matches mockUser.id
    imageUrls: []
  };

  const otherSellerProduct: Product = {
    id: 'prod-2',
    name: 'Leather Belt',
    description: 'Genuine leather',
    price: 35.00,
    quantity: 5,
    sellerId: 'seller-999', // Belongs to someone else
    imageUrls: []
  };

  beforeEach(async () => {
    // 1. Create spies for both services
    productServiceSpy = jasmine.createSpyObj<ProductService>('ProductService', ['getProducts', 'deleteProduct']);
    userServiceSpy = jasmine.createSpyObj<UserService>('UserService', ['getCurrentUser']);

    // 2. Configure testing module
    await TestBed.configureTestingModule({
      imports: [SellerProductsComponent],
      providers: [
        provideRouter([]),
        { provide: ProductService, useValue: productServiceSpy },
        { provide: UserService, useValue: userServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SellerProductsComponent);
    component = fixture.componentInstance;
  });

  // =========================================================================
  // 1. Initialization and filtering products by sellerId
  // =========================================================================
  it('should load current user and only display products owned by this seller', () => {
    // ARRANGE:
    userServiceSpy.getCurrentUser.and.returnValue(of(mockUser));
    productServiceSpy.getProducts.and.returnValue(of([sellerProduct, otherSellerProduct]));

    // ACT:
    fixture.detectChanges();

    // ASSERT:
    expect(component.isLoading).toBeFalse();
    expect(component.sellerId).toBe('seller-123');
    // Only sellerProduct should remain after filtering
    expect(component.products.length).toBe(1);
    expect(component.products[0]).toEqual(sellerProduct);

    // Check inventory statistics calculation
    expect(component.totalUnits).toBe(10);
    expect(component.availableCount).toBe(1);
  });

  // =========================================================================
  // 2. Error handling when user profile fails to load
  // =========================================================================
  it('should set error message when user details fail to load', () => {
    // ARRANGE:
    userServiceSpy.getCurrentUser.and.returnValue(
      throwError(() => new Error('Unauthorized'))
    );

    // ACT:
    fixture.detectChanges();

    // ASSERT:
    expect(component.isLoading).toBeFalse();
    expect(component.errorMessage).toBeTruthy();
    expect(productServiceSpy.getProducts).not.toHaveBeenCalled();
  });

  // =========================================================================
  // 3. Deleting a product (User confirms)
  // =========================================================================
  it('should delete product and remove it from list when confirmed', () => {
    // Setup initial state with 1 product
    userServiceSpy.getCurrentUser.and.returnValue(of(mockUser));
    productServiceSpy.getProducts.and.returnValue(of([sellerProduct]));
    fixture.detectChanges();

    // Mock browser confirm dialog to return true (user clicked OK)
    spyOn(window, 'confirm').and.returnValue(true);
    productServiceSpy.deleteProduct.and.returnValue(of(undefined as unknown as void));

    // ACT: Call deleteProduct
    component.deleteProduct(sellerProduct);

    // ASSERT:
    // Verify deleteProduct was called with the product ID
    expect(productServiceSpy.deleteProduct).toHaveBeenCalledWith('prod-1');
    // Product should be removed from the local products array
    expect(component.products.length).toBe(0);
    expect(component.totalUnits).toBe(0);
  });

  // =========================================================================
  // 4. Canceling deletion (User cancels)
  // =========================================================================
  it('should not delete product when user cancels the confirmation dialog', () => {
    userServiceSpy.getCurrentUser.and.returnValue(of(mockUser));
    productServiceSpy.getProducts.and.returnValue(of([sellerProduct]));
    fixture.detectChanges();

    // Mock browser confirm dialog to return false (user clicked Cancel)
    spyOn(window, 'confirm').and.returnValue(false);

    // ACT:
    component.deleteProduct(sellerProduct);

    // ASSERT:
    expect(productServiceSpy.deleteProduct).not.toHaveBeenCalled();
    expect(component.products.length).toBe(1);
  });
});
