import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { Order } from '../../models/order.model';
import { OrderSearchFilters, OrderService } from '../../services/order.service';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.css'
})
export class OrdersComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);

  orders: Order[] = [];
  selectedOrder: Order | null = null;
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
    const orderId = this.route.snapshot.paramMap.get('orderId');
    if (orderId) {
      this.loadOrder(orderId);
    } else {
      this.loadOrders();
    }
  }

  loadOrders(): void
  {
    this.isLoading = true;
    this.errorMessage = '';
    this.orderService.getBuyerOrders(this.filters()).subscribe({
      next: (orders) => {
        this.orders = orders;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'Your orders could not be loaded.');
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

  cancel(order: Order): void
  {
    this.busyOrderId = order.id;
    this.errorMessage = '';
    this.orderService.cancelOrder(order.id).subscribe({
      next: (updated) => {
        this.orders = this.orders.map((item) => item.id === updated.id ? updated : item);
        if (this.selectedOrder?.id === updated.id) {
          this.selectedOrder = updated;
        }
        this.busyOrderId = '';
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'This order could not be cancelled.');
        this.busyOrderId = '';
      }
    });
  }

  archive(order: Order): void
  {
    this.busyOrderId = order.id;
    this.errorMessage = '';
    this.orderService.archiveOrder(order.id).subscribe({
      next: () => {
        this.orders = this.orders.filter((item) => item.id !== order.id);
        this.selectedOrder = null;
        this.busyOrderId = '';
        this.loadOrders();
        void this.router.navigateByUrl('/orders');
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'This order could not be removed.');
        this.busyOrderId = '';
      }
    });
  }

  reorder(order: Order): void
  {
    this.busyOrderId = order.id;
    this.errorMessage = '';
    this.orderService.reorder(order.id).subscribe({
      next: () => {
        this.busyOrderId = '';
        this.successMessage = 'Available items were added to your cart.';
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'This order could not be reordered.');
        this.busyOrderId = '';
      }
    });
  }

  statusReached(current: Order['status'], step: string): boolean
  {
    if (current === 'CANCELLED') {
      return false;
    }
    const progress = ['PLACED', 'PROCESSING', 'SHIPPED', 'DELIVERED'];
    return progress.indexOf(current) >= progress.indexOf(step);
  }

  private loadOrder(orderId: string): void
  {
    this.orderService.getOrder(orderId).subscribe({
      next: (order) => {
        this.selectedOrder = order;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'This order could not be loaded.');
        this.isLoading = false;
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
