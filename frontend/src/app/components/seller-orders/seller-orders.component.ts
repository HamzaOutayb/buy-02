import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { Order, OrderStatus } from '../../models/order.model';
import { OrderSearchFilters, OrderService } from '../../services/order.service';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './seller-orders.component.html',
  styleUrl: './seller-orders.component.css'
})
export class SellerOrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  orders: Order[] = [];
  isLoading = true;
  busyOrderId = '';
  errorMessage = '';
  successMessage = '';
  searchText = '';
  categoryFilter = '';
  statusFilter = '';
  fromDate = '';
  toDate = '';

  get filteredOrders(): Order[]
  {
    const category = this.categoryFilter.trim().toLowerCase();
    if (!category) {
      return this.orders;
    }

    return this.orders.filter((order) =>
      order.items.some((item) =>
        (item.category ?? '').trim().toLowerCase() === category
      )
    );
  }

  ngOnInit(): void
  {
    this.loadOrders();
  }

  loadOrders(): void
  {
    this.isLoading = true;
    this.errorMessage = '';
    this.orderService.getSellerOrders(this.filters()).subscribe({
      next: (orders) => {
        this.orders = orders;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'Seller orders could not be loaded.');
        this.isLoading = false;
      }
    });
  }

  applyFilters(): void
  {
    this.loadOrders();
  }

  clearFilters(): void
  {
    this.searchText = '';
    this.categoryFilter = '';
    this.statusFilter = '';
    this.fromDate = '';
    this.toDate = '';
    this.loadOrders();
  }

  nextStatus(order: Order): OrderStatus | null
  {
    switch (order.status) {
      case 'PLACED': return 'PROCESSING';
      case 'PROCESSING': return 'SHIPPED';
      case 'SHIPPED': return 'DELIVERED';
      default: return null;
    }
  }

  advance(order: Order): void
  {
    const status = this.nextStatus(order);
    if (!status) {
      return;
    }
    this.busyOrderId = order.id;
    this.errorMessage = '';
    this.orderService.updateStatus(order.id, status).subscribe({
      next: (updated) => {
        this.orders = this.orders.map((item) => item.id === updated.id ? updated : item);
        this.busyOrderId = '';
        this.successMessage = `Order moved to ${updated.status}.`;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'Order status could not be updated.');
        this.busyOrderId = '';
      }
    });
  }

  private filters(): OrderSearchFilters
  {
    return {
      q: this.searchText,
      status: this.statusFilter,
      from: this.fromDate,
      to: this.toDate
    };
  }
}
