import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../helpers/api-url';
import { CheckoutRequest, Order, OrderStatus } from '../models/order.model';

export interface OrderSearchFilters {
  q: string;
  status: string;
  from: string;
  to: string;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  checkout(request: CheckoutRequest): Observable<Order[]>
  {
    return this.http.post<Order[]>(`${API_BASE_URL}/orders/checkout`, request);
  }

  getBuyerOrders(filters: OrderSearchFilters): Observable<Order[]>
  {
    return this.http.get<Order[]>(`${API_BASE_URL}/orders`, { params: this.queryParams(filters) });
  }

  getOrder(orderId: string): Observable<Order>
  {
    return this.http.get<Order>(`${API_BASE_URL}/orders/${encodeURIComponent(orderId)}`);
  }

  cancelOrder(orderId: string): Observable<Order>
  {
    return this.http.post<Order>(
      `${API_BASE_URL}/orders/${encodeURIComponent(orderId)}/cancel`,
      {}
    );
  }

  archiveOrder(orderId: string): Observable<void>
  {
    return this.http.delete<void>(`${API_BASE_URL}/orders/${encodeURIComponent(orderId)}`);
  }

  getSellerOrders(filters: OrderSearchFilters): Observable<Order[]>
  {
    return this.http.get<Order[]>(`${API_BASE_URL}/seller/orders`, { params: this.queryParams(filters) });
  }

  updateStatus(orderId: string, status: OrderStatus): Observable<Order>
  {
    return this.http.patch<Order>(
      `${API_BASE_URL}/seller/orders/${encodeURIComponent(orderId)}/status`,
      { status }
    );
  }

  private queryParams(filters: OrderSearchFilters): Record<string, string>
  {
    return Object.fromEntries(
      Object.entries(filters).filter(([, value]) => value.trim().length > 0)
    );
  }
}
