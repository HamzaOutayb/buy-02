/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Order } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { OrdersComponent } from './orders.component';

describe('OrdersComponent', () => {
  let fixture: ComponentFixture<OrdersComponent>;
  let component: OrdersComponent;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;

  const orders: Order[] = [
    {
      id: 'order-1',
      buyerId: 'buyer-1',
      sellerId: 'seller-1',
      items: [{
        productId: 'product-1',
        productName: 'Handmade mug',
        category: 'Home',
        sellerId: 'seller-1',
        unitPrice: 15,
        quantity: 1,
        lineTotal: 15
      }],
      deliveryAddress: {
        recipientName: 'Customer',
        phone: '123456789',
        street: '1 Main St',
        city: 'Town',
        postalCode: '12345'
      },
      total: 15,
      paymentMethod: 'PAY_ON_DELIVERY',
      status: 'PLACED',
      createdAt: '2026-01-01T00:00:00Z'
    },
    {
      id: 'order-2',
      buyerId: 'buyer-1',
      sellerId: 'seller-2',
      items: [{
        productId: 'product-2',
        productName: 'Leather belt',
        category: 'Accessories',
        sellerId: 'seller-2',
        unitPrice: 35,
        quantity: 1,
        lineTotal: 35
      }],
      deliveryAddress: {
        recipientName: 'Customer',
        phone: '123456789',
        street: '1 Main St',
        city: 'Town',
        postalCode: '12345'
      },
      total: 35,
      paymentMethod: 'PAY_ON_DELIVERY',
      status: 'PLACED',
      createdAt: '2026-01-02T00:00:00Z'
    }
  ];

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj<OrderService>(
      'OrderService',
      ['getBuyerOrders', 'getOrder', 'cancelOrder', 'archiveOrder', 'reorder']
    );
    orderServiceSpy.getBuyerOrders.and.returnValue(of(orders));

    await TestBed.configureTestingModule({
      imports: [OrdersComponent],
      providers: [
        provideRouter([]),
        { provide: OrderService, useValue: orderServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('filters customer orders by the category of their items', () => {
    component.categoryFilter = ' home ';

    expect(component.filteredOrders.map((order) => order.id)).toEqual(['order-1']);
  });
});
