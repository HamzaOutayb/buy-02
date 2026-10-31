/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Order } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { SellerOrdersComponent } from './seller-orders.component';

describe('SellerOrdersComponent', () => {
  let fixture: ComponentFixture<SellerOrdersComponent>;
  let component: SellerOrdersComponent;
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
      buyerId: 'buyer-2',
      sellerId: 'seller-1',
      items: [{
        productId: 'product-2',
        productName: 'Desk lamp',
        category: 'Lighting',
        sellerId: 'seller-1',
        unitPrice: 40,
        quantity: 1,
        lineTotal: 40
      }],
      deliveryAddress: {
        recipientName: 'Customer',
        phone: '123456789',
        street: '1 Main St',
        city: 'Town',
        postalCode: '12345'
      },
      total: 40,
      paymentMethod: 'PAY_ON_DELIVERY',
      status: 'PROCESSING',
      createdAt: '2026-01-02T00:00:00Z'
    }
  ];

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj<OrderService>(
      'OrderService',
      ['getSellerOrders', 'updateStatus']
    );
    orderServiceSpy.getSellerOrders.and.returnValue(of(orders));

    await TestBed.configureTestingModule({
      imports: [SellerOrdersComponent],
      providers: [{ provide: OrderService, useValue: orderServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(SellerOrdersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('filters seller orders by the category of their items', () => {
    component.categoryFilter = 'lighting';

    expect(component.filteredOrders.map((order) => order.id)).toEqual(['order-2']);
  });
});
