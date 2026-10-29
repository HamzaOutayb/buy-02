/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Product, ProductPage } from '../../models/product.model';
import { ProductService } from '../../services/product.service';
import { ProductsComponent } from './products.component';

/**
 * ProductsComponent Unit Tests (Angular / Jasmine)
 *
 * Why Test the Component?
 * - We verify the component's state (isLoading, hasError, products array)
 *   and behavior when the ProductService succeeds or fails.
 *
 * Key Concepts:
 * - ComponentFixture: Manages the component instance and its rendered DOM template.
 * - jasmine.createSpyObj: Creates a mock ProductService so we don't make real HTTP calls.
 * - fixture.detectChanges(): Triggers Angular's change detection and lifecycle hooks (e.g. ngOnInit).
 */
describe('ProductsComponent', () => {
  let fixture: ComponentFixture<ProductsComponent>;
  let component: ProductsComponent;
  let productServiceSpy: jasmine.SpyObj<ProductService>;

  const mockProducts: Product[] = [
    {
      id: 'prod-1',
      name: 'Mechanical Keyboard',
      description: 'RGB mechanical keyboard',
      price: 89.99,
      quantity: 25,
      sellerId: 'seller-1',
      imageUrls: ['https://example.com/keyboard.jpg']
    },
    {
      id: 'prod-2',
      name: 'Gaming Mouse',
      description: 'Precision gaming mouse',
      price: 49.99,
      quantity: 15,
      sellerId: 'seller-2',
      imageUrls: []
    }
  ];

  const productPage: ProductPage = {
    content: mockProducts,
    page: 0,
    size: 12,
    totalElements: 2,
    totalPages: 1
  };

  beforeEach(async () => {
    productServiceSpy = jasmine.createSpyObj<ProductService>(
      'ProductService',
      ['searchProducts']
    );

    // 2. Configure the testing module with the standalone component and our mock service
    await TestBed.configureTestingModule({
      imports: [ProductsComponent],
      providers: [
        provideRouter([]), // Provides mock routing support for RouterLink
        { provide: ProductService, useValue: productServiceSpy } // Inject our mock service
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductsComponent);
    component = fixture.componentInstance;
  });

  // =========================================================================
  // 1. Component creation & successful product loading
  // =========================================================================
  it('should create the component and load products on initialization', () => {
    // ARRANGE: Tell the spy to return our mock products as an Observable
    productServiceSpy.searchProducts.and.returnValue(of(productPage));

    // ACT: Run change detection which executes ngOnInit() -> loadProducts()
    fixture.detectChanges();

    // ASSERT:
    // Check that component state updated correctly
    expect(component).toBeTruthy();
    expect(component.isLoading).toBeFalse();
    expect(component.hasError).toBeFalse();
    expect(component.products.length).toBe(2);
    expect(component.products).toEqual(mockProducts);

    // Verify getProducts was called once
    expect(productServiceSpy.searchProducts).toHaveBeenCalledTimes(1);
  });

  // =========================================================================
  // 2. Error handling when product fetch fails
  // =========================================================================
  it('should set hasError to true and stop loading when fetching products fails', () => {
    // ARRANGE: Simulate an API error
    productServiceSpy.searchProducts.and.returnValue(
      throwError(() => new Error('Network error'))
    );

    // ACT: Trigger ngOnInit()
    fixture.detectChanges();

    // ASSERT:
    expect(component.isLoading).toBeFalse();
    expect(component.hasError).toBeTrue();
    expect(component.products.length).toBe(0);
  });

  // =========================================================================
  // 3. Handling empty product catalog
  // =========================================================================
  it('should display empty list when no products are returned', () => {
    // ARRANGE: Return an empty list
    productServiceSpy.searchProducts.and.returnValue(of({
      content: [],
      page: 0,
      size: 12,
      totalElements: 0,
      totalPages: 0
    }));

    // ACT:
    fixture.detectChanges();

    // ASSERT:
    expect(component.isLoading).toBeFalse();
    expect(component.hasError).toBeFalse();
    expect(component.products).toEqual([]);
  });

  // =========================================================================
  // 4. Retrying product load (e.g. "Try again" button click)
  // =========================================================================
  it('should retry loading products when loadProducts() is called again', () => {
    // ARRANGE: First attempt fails
    productServiceSpy.searchProducts.and.returnValue(
      throwError(() => new Error('Server down'))
    );
    fixture.detectChanges();
    expect(component.hasError).toBeTrue();

    // ACT: Second attempt succeeds when user retries
    productServiceSpy.searchProducts.and.returnValue(of(productPage));
    component.loadProducts();

    // ASSERT:
    expect(component.isLoading).toBeFalse();
    expect(component.hasError).toBeFalse();
    expect(component.products).toEqual(mockProducts);
    expect(productServiceSpy.searchProducts).toHaveBeenCalledTimes(2);
  });

  it('should send current filters and reset pagination when applying search', () => {
    productServiceSpy.searchProducts.and.returnValue(of(productPage));
    fixture.detectChanges();

    component.searchTerm = ' keyboard ';
    component.category = 'Electronics';
    component.page = 2;
    component.applyFilters();

    expect(component.page).toBe(0);
    expect(productServiceSpy.searchProducts).toHaveBeenCalledWith({
      q: 'keyboard',
      category: 'Electronics',
      page: 0,
      size: 12
    });
  });

  it('should clear the keyword and category filters', () => {
    productServiceSpy.searchProducts.and.returnValue(of(productPage));
    fixture.detectChanges();

    component.searchTerm = 'keyboard';
    component.category = 'Electronics';
    component.clearFilters();

    expect(component.searchTerm).toBe('');
    expect(component.category).toBe('');
    expect(component.page).toBe(0);
    expect(productServiceSpy.searchProducts).toHaveBeenCalledWith({
      q: '',
      category: '',
      page: 0,
      size: 12
    });
  });
});
