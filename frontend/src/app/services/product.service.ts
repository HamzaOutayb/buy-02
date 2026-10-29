import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Product,
  ProductInput,
  ProductPage,
  ProductSearchFilters
} from '../models/product.model';
import { API_BASE_URL } from '../helpers/api-url';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly http = inject(HttpClient);

  getProducts(): Observable<Product[]>
  {
    return this.http.get<Product[]>(`${API_BASE_URL}/products`);
  }

  searchProducts(filters: ProductSearchFilters): Observable<ProductPage>
  {
    let params = new HttpParams();

    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value);
      }
    }

    return this.http.get<ProductPage>(`${API_BASE_URL}/products/search`, { params });
  }

  getProduct(productId: string): Observable<Product>
  {
    return this.http.get<Product>(`${API_BASE_URL}/products/${productId}`);
  }

  createProduct(product: ProductInput): Observable<Product>
  {
    return this.http.post<Product>(`${API_BASE_URL}/products`, product);
  }

  updateProduct(productId: string, product: ProductInput): Observable<Product>
  {
    return this.http.put<Product>(
      `${API_BASE_URL}/products/${productId}`,
      product
    );
  }

  deleteProduct(productId: string): Observable<void>
  {
    return this.http.delete<void>(`${API_BASE_URL}/products/${productId}`);
  }
}
