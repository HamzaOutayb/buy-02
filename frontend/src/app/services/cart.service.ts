import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../helpers/api-url';
import { Cart } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);

  getCart(): Observable<Cart>
  {
    return this.http.get<Cart>(`${API_BASE_URL}/cart`);
  }

  addItem(productId: string, quantity = 1): Observable<Cart>
  {
    return this.http.post<Cart>(`${API_BASE_URL}/cart/items`, { productId, quantity });
  }

  updateQuantity(productId: string, quantity: number): Observable<Cart>
  {
    return this.http.put<Cart>(`${API_BASE_URL}/cart/items/${encodeURIComponent(productId)}`, { quantity });
  }

  removeItem(productId: string): Observable<Cart>
  {
    return this.http.delete<Cart>(`${API_BASE_URL}/cart/items/${encodeURIComponent(productId)}`);
  }
}
