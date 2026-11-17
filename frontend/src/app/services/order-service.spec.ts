import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  CreateOrderReq,
  Order,
  OrderPage,
  OrderService,
  SellerSalesAnalytics,
  statusBadgeClass,
} from './order-service';
import { environment } from '../../environments/environment';

describe('OrderService', () => {
  let service: OrderService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/orders`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OrderService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrderService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should post create order', () => {
    const req: CreateOrderReq = {
      items: [{ productId: 'p1', quantity: 2 }],
      deliveryAddress: {
        recipientName: 'Alice',
        phone: '1234567890',
        city: 'City',
        fullAddress: 'Address 1',
      },
      paymentMethod: 'CASH_ON_DELIVERY',
    };
    const mockOrder = { id: 'o1', orderReference: 'ORD-123' } as Order;

    service.createOrder(req).subscribe((order) => {
      expect(order.id).toBe('o1');
      expect(order.orderReference).toBe('ORD-123');
    });

    const reqTest = httpTesting.expectOne(baseUrl);
    expect(reqTest.request.method).toBe('POST');
    expect(reqTest.request.body).toEqual(req);
    reqTest.flush(mockOrder);
  });

  it('should get customer orders with params', () => {
    const mockPage: OrderPage = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 10,
    };

    service.getMyOrders({ status: 'PENDING', page: 0, size: 10 }).subscribe((page) => {
      expect(page.content.length).toBe(0);
    });

    const reqTest = httpTesting.expectOne(`${baseUrl}?status=PENDING&page=0&size=10`);
    expect(reqTest.request.method).toBe('GET');
    reqTest.flush(mockPage);
  });

  it('should cancel customer order via POST', () => {
    const mockOrder = { id: 'o1', status: 'CANCELLED' } as Order;

    service.cancelOrder('o1').subscribe((order) => {
      expect(order.status).toBe('CANCELLED');
    });

    const reqTest = httpTesting.expectOne(`${baseUrl}/o1/cancel`);
    expect(reqTest.request.method).toBe('POST');
    reqTest.flush(mockOrder);
  });

  it('should reorder previous order via POST', () => {
    const mockOrder = { id: 'o2', status: 'PENDING' } as Order;

    service.reorder('o1').subscribe((order) => {
      expect(order.id).toBe('o2');
    });

    const reqTest = httpTesting.expectOne(`${baseUrl}/o1/reorder`);
    expect(reqTest.request.method).toBe('POST');
    reqTest.flush(mockOrder);
  });

  it('should get seller orders with query params', () => {
    const mockPage: OrderPage = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 10,
    };

    service.getSellerOrders({ customer: 'cust1', page: 1, size: 10 }).subscribe((page) => {
      expect(page.totalElements).toBe(0);
    });

    const reqTest = httpTesting.expectOne(`${baseUrl}/seller?customer=cust1&page=1&size=10`);
    expect(reqTest.request.method).toBe('GET');
    reqTest.flush(mockPage);
  });

  it('should load seller sales analytics', () => {
    const analytics: SellerSalesAnalytics = {
      totalOrders: 4,
      pendingOrders: 1,
      completedOrders: 2,
      unitsSold: 5,
      revenue: 125,
    };

    service.getSellerAnalytics().subscribe((result) => expect(result).toEqual(analytics));

    const reqTest = httpTesting.expectOne(`${baseUrl}/seller/analytics`);
    expect(reqTest.request.method).toBe('GET');
    reqTest.flush(analytics);
  });

  it('should confirm seller order via POST', () => {
    const mockOrder = { id: 'o1', status: 'CONFIRMED' } as Order;

    service.confirmOrder('o1').subscribe((order) => {
      expect(order.status).toBe('CONFIRMED');
    });

    const reqTest = httpTesting.expectOne(`${baseUrl}/seller/o1/confirm`);
    expect(reqTest.request.method).toBe('POST');
    reqTest.flush(mockOrder);
  });

  it('should update seller status via PUT', () => {
    const mockOrder = { id: 'o1', status: 'SHIPPED' } as Order;

    service.updateOrderStatus('o1', 'SHIPPED').subscribe((order) => {
      expect(order.status).toBe('SHIPPED');
    });

    const reqTest = httpTesting.expectOne(`${baseUrl}/seller/o1/status`);
    expect(reqTest.request.method).toBe('PUT');
    expect(reqTest.request.body).toEqual({ status: 'SHIPPED' });
    reqTest.flush(mockOrder);
  });

  it('should cancel seller order via POST', () => {
    const mockOrder = { id: 'o1', status: 'CANCELLED' } as Order;

    service.cancelSellerOrder('o1').subscribe((order) => {
      expect(order.status).toBe('CANCELLED');
    });

    const reqTest = httpTesting.expectOne(`${baseUrl}/seller/o1/cancel`);
    expect(reqTest.request.method).toBe('POST');
    reqTest.flush(mockOrder);
  });

  it('should return correct badge classes for order statuses', () => {
    expect(statusBadgeClass('PENDING')).toContain('amber');
    expect(statusBadgeClass('CONFIRMED')).toContain('blue');
    expect(statusBadgeClass('PROCESSING')).toContain('purple');
    expect(statusBadgeClass('SHIPPED')).toContain('indigo');
    expect(statusBadgeClass('DELIVERED')).toContain('teal');
    expect(statusBadgeClass('CANCELLED')).toContain('red');
  });
});
