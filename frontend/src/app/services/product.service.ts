import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Product, ProductInput } from '../models/product.model';
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
