import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { Product, ProductRequest, ProductService } from './product-service';

describe('ProductService', () => {
  let service: ProductService;
  let httpTesting: HttpTestingController;
  const apiUrl = 'https://localhost:8080/api/v1/products';
  const product: Product = {
    id: 'product-1',
    name: 'Chair',
    description: 'Wood chair',
    price: 20,
    quantity: 3,
    userId: 'seller-1',
    imageUrls: ['https://cdn.example/chair.png'],
    categories: ['furniture'],
  };
  const request: ProductRequest = {
    name: product.name,
    description: product.description,
    price: product.price,
    quantity: product.quantity,
    imageUrls: product.imageUrls,
    categories: product.categories,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('gets all products', () => {
    let actual: Product[] | undefined;
    service.getProducts().subscribe((value) => (actual = value));
    const req = httpTesting.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush([product]);
    expect(actual).toEqual([product]);
  });

  it('gets the current seller products', () => {
    service.getMyProducts().subscribe();
    const req = httpTesting.expectOne(`${apiUrl}/mine`);
    expect(req.request.method).toBe('GET');
    req.flush([product]);
  });

  it('gets a product by id', () => {
    service.getProduct('product-1').subscribe((actual) => expect(actual).toEqual(product));
    const req = httpTesting.expectOne(`${apiUrl}/product-1`);
    expect(req.request.method).toBe('GET');
    req.flush(product);
  });

  it('creates a product with the supplied request', () => {
    service.createProduct(request).subscribe((actual) => expect(actual).toEqual(product));
    const req = httpTesting.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(product);
  });

  it('updates a product with the supplied request', () => {
    service.updateProduct('product-1', request).subscribe((actual) => expect(actual).toEqual(product));
    const req = httpTesting.expectOne(`${apiUrl}/product-1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(product);
  });

  it('deletes a product by id', () => {
    let completed = false;
    service.deleteProduct('product-1').subscribe(() => (completed = true));
    const req = httpTesting.expectOne(`${apiUrl}/product-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    expect(completed).toBe(true);
  });
});
