/// <reference types="jasmine" />

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../helpers/api-url';
import { Product, ProductInput } from '../models/product.model';
import { ProductService } from './product.service';

/**
 * ProductService Unit Tests (Angular / Jasmine)
 *
 * Why Test the Angular Service?
 * - We verify that ProductService makes the correct HTTP requests (GET, POST, PUT, DELETE)
 *   to the expected backend endpoints without actually sending real network traffic.
 *
 * Key Tools:
 * - TestBed: The primary Angular testing utility to configure dependencies.
 * - HttpTestingController: Intercepts HTTP calls made by HttpClient so we can assert on
 *   the request URL, HTTP method, request body, and provide mock responses via .flush().
 */
describe('ProductService', () => {
  let productService: ProductService;
  let httpTestingController: HttpTestingController;

  // Reusable dummy product for testing
  const mockProduct: Product = {
    id: 'prod-123',
    name: 'Wireless Mouse',
    description: 'Ergonomic wireless mouse',
    price: 29.99,
    quantity: 50,
    sellerId: 'seller-1',
    imageUrls: ['https://example.com/mouse.jpg']
  };

  beforeEach(() => {
    // Configure a testing module with HttpClient testing support
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    // Inject the service and the HTTP testing mock controller
    productService = TestBed.inject(ProductService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify that no extra or unexpected HTTP requests were made
    httpTestingController.verify();
  });

  // =========================================================================
  // 1. getProducts()
  // =========================================================================
  it('should fetch all products via GET /products', () => {
    // 1. ACT: Call the service method and subscribe to the returned Observable
    let actualProducts: Product[] = [];
    productService.getProducts().subscribe((products) => {
      actualProducts = products;
    });

    // 2. EXPECT: Verify that an HTTP GET request was made to the correct URL
    const req = httpTestingController.expectOne(`${API_BASE_URL}/products`);
    expect(req.request.method).toBe('GET');

    // 3. FLUSH: Simulate the backend returning data
    req.flush([mockProduct]);

    // 4. ASSERT: Ensure the subscriber received the expected data
    expect(actualProducts.length).toBe(1);
    expect(actualProducts[0]).toEqual(mockProduct);
  });

  it('should search products with filters and pagination parameters', () => {
    let actualPage: unknown;
    productService.searchProducts({
      q: 'keyboard',
      category: 'Electronics',
      page: 1,
      size: 12
    }).subscribe((page) => {
      actualPage = page;
    });

    const req = httpTestingController.expectOne(
      (request) => request.url === `${API_BASE_URL}/products/search`
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('q')).toBe('keyboard');
    expect(req.request.params.get('category')).toBe('Electronics');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('12');

    const response = {
      content: [mockProduct],
      page: 1,
      size: 12,
      totalElements: 13,
      totalPages: 2
    };
    req.flush(response);
    expect(actualPage).toEqual(response);
  });

  // =========================================================================
  // 2. getProduct(id)
  // =========================================================================
  it('should fetch a single product by ID via GET /products/:id', () => {
    let actualProduct: Product | undefined;
    productService.getProduct('prod-123').subscribe((product) => {
      actualProduct = product;
    });

    // Expect GET request to /products/prod-123
    const req = httpTestingController.expectOne(`${API_BASE_URL}/products/prod-123`);
    expect(req.request.method).toBe('GET');

    // Return the mock product
    req.flush(mockProduct);

    expect(actualProduct).toEqual(mockProduct);
  });

  // =========================================================================
  // 3. createProduct(input)
  // =========================================================================
  it('should create a new product via POST /products', () => {
    const input: ProductInput = {
      name: 'Wireless Mouse',
      description: 'Ergonomic wireless mouse',
      price: 29.99,
      quantity: 50,
      imageUrls: ['https://example.com/mouse.jpg']
    };

    let createdProduct: Product | undefined;
    productService.createProduct(input).subscribe((product) => {
      createdProduct = product;
    });

    // Expect POST request to /products
    const req = httpTestingController.expectOne(`${API_BASE_URL}/products`);
    expect(req.request.method).toBe('POST');
    // Ensure the payload matches our input
    expect(req.request.body).toEqual(input);

    // Return mock created product
    req.flush(mockProduct);

    expect(createdProduct).toEqual(mockProduct);
  });

  // =========================================================================
  // 4. updateProduct(id, input)
  // =========================================================================
  it('should update an existing product via PUT /products/:id', () => {
    const updateInput: ProductInput = {
      name: 'Updated Mouse',
      description: 'Updated description',
      price: 34.99,
      quantity: 40,
      imageUrls: []
    };

    let updatedProduct: Product | undefined;
    productService.updateProduct('prod-123', updateInput).subscribe((product) => {
      updatedProduct = product;
    });

    // Expect PUT request to /products/prod-123
    const req = httpTestingController.expectOne(`${API_BASE_URL}/products/prod-123`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updateInput);

    const expectedResponse: Product = { ...mockProduct, ...updateInput };
    req.flush(expectedResponse);

    expect(updatedProduct).toEqual(expectedResponse);
  });

  // =========================================================================
  // 5. deleteProduct(id)
  // =========================================================================
  it('should delete a product by ID via DELETE /products/:id', () => {
    let completed = false;
    productService.deleteProduct('prod-123').subscribe({
      next: () => {
        completed = true;
      }
    });

    // Expect DELETE request to /products/prod-123
    const req = httpTestingController.expectOne(`${API_BASE_URL}/products/prod-123`);
    expect(req.request.method).toBe('DELETE');

    // Simulate 204 No Content response
    req.flush(null);

    expect(completed).toBeTrue();
  });
});
