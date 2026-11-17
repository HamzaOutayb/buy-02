import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DeliveryAddress {
  recipientName: string;
  phone: string;
  city: string;
  fullAddress: string;
}

export interface OrderItemReq {
  productId: string;
  quantity: number;
}

export interface CreateOrderReq {
  items: OrderItemReq[];
  deliveryAddress: DeliveryAddress;
}

export interface OrderItem {
  productId: string;
  sellerId: string;
  productName: string;
  imageUrl: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED';
export type PaymentMethod = 'CASH_ON_DELIVERY';
export type PaymentStatus = 'PENDING' | 'PAID';

export interface Order {
  id: string;
  orderReference: string;
  customerId: string;
  status: OrderStatus;
  items: OrderItem[];
  deliveryAddress: DeliveryAddress;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  subtotal: number;
  shippingFee: number;
  total: number;
  createdAt: string;
  updatedAt: string;
}

export interface OrderSummary {
  id: string;
  orderReference: string;
  customerId: string;
  status: OrderStatus;
  itemCount: number;
  total: number;
  paymentMethod: PaymentMethod;
  createdAt: string;
}

export interface SellerSalesAnalytics {
  totalOrders: number;
  pendingOrders: number;
  completedOrders: number;
  unitsSold: number;
  revenue: number;
}

export interface OrderPage {
  content: OrderSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export function statusBadgeClass(status: OrderStatus): string {
  switch (status) {
    case 'PENDING':
      return 'bg-amber-100 text-amber-800 border-amber-300';
    case 'CONFIRMED':
      return 'bg-blue-100 text-blue-800 border-blue-300';
    case 'PROCESSING':
      return 'bg-purple-100 text-purple-800 border-purple-300';
    case 'SHIPPED':
      return 'bg-indigo-100 text-indigo-800 border-indigo-300';
    case 'DELIVERED':
      return 'bg-teal-100 text-teal-800 border-teal-300';
    case 'CANCELLED':
      return 'bg-red-100 text-red-800 border-red-300';
    default:
      return 'bg-slate-100 text-slate-800 border-slate-300';
  }
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/orders`;

  // ── Customer ────────────────────────────────────────────────────────────────

  createOrder(req: CreateOrderReq): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, req);
  }

  getMyOrders(params: {
    status?: string;
    orderReference?: string;
    dateFrom?: string;
    dateTo?: string;
    page?: number;
    size?: number;
  }): Observable<OrderPage> {
    let p = new HttpParams();
    if (params.status) p = p.set('status', params.status);
    if (params.orderReference) p = p.set('orderReference', params.orderReference);
    if (params.dateFrom) p = p.set('dateFrom', params.dateFrom);
    if (params.dateTo) p = p.set('dateTo', params.dateTo);
    if (params.page !== undefined) p = p.set('page', params.page);
    if (params.size !== undefined) p = p.set('size', params.size);
    return this.http.get<OrderPage>(this.apiUrl, { params: p });
  }

  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  cancelOrder(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/cancel`, {});
  }

  reorder(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/reorder`, {});
  }

  // ── Seller ──────────────────────────────────────────────────────────────────

  getSellerOrders(params: {
    status?: string;
    customer?: string;
    orderReference?: string;
    dateFrom?: string;
    dateTo?: string;
    page?: number;
    size?: number;
  }): Observable<OrderPage> {
    let p = new HttpParams();
    if (params.status) p = p.set('status', params.status);
    if (params.customer) p = p.set('customer', params.customer);
    if (params.orderReference) p = p.set('orderReference', params.orderReference);
    if (params.dateFrom) p = p.set('dateFrom', params.dateFrom);
    if (params.dateTo) p = p.set('dateTo', params.dateTo);
    if (params.page !== undefined) p = p.set('page', params.page);
    if (params.size !== undefined) p = p.set('size', params.size);
    return this.http.get<OrderPage>(`${this.apiUrl}/seller`, { params: p });
  }

  getSellerAnalytics(): Observable<SellerSalesAnalytics> {
    return this.http.get<SellerSalesAnalytics>(`${this.apiUrl}/seller/analytics`);
  }

  getSellerOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/seller/${id}`);
  }

  confirmOrder(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/seller/${id}/confirm`, {});
  }

  updateOrderStatus(id: string, status: OrderStatus): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/seller/${id}/status`, { status });
  }

  cancelSellerOrder(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/seller/${id}/cancel`, {});
  }
}
