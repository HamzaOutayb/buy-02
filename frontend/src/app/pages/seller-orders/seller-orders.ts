import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, NgClass, SlicePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  Order,
  OrderPage,
  OrderService,
  OrderStatus,
  OrderSummary,
  statusBadgeClass,
} from '../../services/order-service';
import { UserContextService } from '../../services/user-context-service';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, NgClass, RouterLink, SlicePipe],
  templateUrl: './seller-orders.html',
  styleUrl: './seller-orders.css',
})
export class SellerOrders implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly userContext = inject(UserContextService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly user = this.userContext.user;
  readonly orders = signal<OrderSummary[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly pageSize = 10;

  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly updatingId = signal<string | null>(null);

  // Selected order for detail modal / view
  readonly selectedOrderDetail = signal<Order | null>(null);
  readonly loadingDetail = signal(false);

  // Filters
  selectedStatus: OrderStatus | '' = '';
  searchCustomer = '';
  searchReference = '';
  dateFrom = '';
  dateTo = '';

  readonly statusOptions: (OrderStatus | '')[] = [
    '',
    'PENDING',
    'CONFIRMED',
    'PROCESSING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED',
  ];

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    const params: {
      status?: string;
      customer?: string;
      orderReference?: string;
      dateFrom?: string;
      dateTo?: string;
      page: number;
      size: number;
    } = {
      page: this.currentPage(),
      size: this.pageSize,
    };

    if (this.selectedStatus) {
      params.status = this.selectedStatus;
    }
    if (this.searchCustomer.trim()) {
      params.customer = this.searchCustomer.trim();
    }
    if (this.searchReference.trim()) {
      params.orderReference = this.searchReference.trim();
    }
    if (this.dateFrom) {
      params.dateFrom = new Date(this.dateFrom).toISOString();
    }
    if (this.dateTo) {
      const d = new Date(this.dateTo);
      d.setHours(23, 59, 59, 999);
      params.dateTo = d.toISOString();
    }

    this.orderService.getSellerOrders(params).subscribe({
      next: (page: OrderPage) => {
        this.orders.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.loadOrders();
  }

  resetFilters(): void {
    this.selectedStatus = '';
    this.searchCustomer = '';
    this.searchReference = '';
    this.dateFrom = '';
    this.dateTo = '';
    this.currentPage.set(0);
    this.loadOrders();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadOrders();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  viewDetails(orderId: string): void {
    this.loadingDetail.set(true);
    this.orderService.getSellerOrderById(orderId).subscribe({
      next: (detail) => {
        this.selectedOrderDetail.set(detail);
        this.loadingDetail.set(false);
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Failed to load order details';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
        this.loadingDetail.set(false);
      },
    });
  }

  closeDetails(): void {
    this.selectedOrderDetail.set(null);
  }

  confirmOrder(order: OrderSummary): void {
    this.updatingId.set(order.id);
    this.orderService.confirmOrder(order.id).subscribe({
      next: () => {
        this.snackBar.open(`Order #${order.orderReference} confirmed`, 'Dismiss', { duration: 4000 });
        this.updatingId.set(null);
        this.loadOrders();
        if (this.selectedOrderDetail()?.id === order.id) {
          this.viewDetails(order.id);
        }
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Failed to confirm order';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
        this.updatingId.set(null);
      },
    });
  }

  updateStatus(order: OrderSummary, newStatus: OrderStatus): void {
    this.updatingId.set(order.id);
    this.orderService.updateOrderStatus(order.id, newStatus).subscribe({
      next: () => {
        this.snackBar.open(`Order status updated to ${newStatus}`, 'Dismiss', { duration: 4000 });
        this.updatingId.set(null);
        this.loadOrders();
        if (this.selectedOrderDetail()?.id === order.id) {
          this.viewDetails(order.id);
        }
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Failed to update order status';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
        this.updatingId.set(null);
      },
    });
  }

  cancelOrder(order: OrderSummary): void {
    if (!window.confirm(`Cancel order #${order.orderReference}?`)) {
      return;
    }

    this.updatingId.set(order.id);
    this.orderService.cancelSellerOrder(order.id).subscribe({
      next: () => {
        this.snackBar.open(`Order #${order.orderReference} cancelled`, 'Dismiss', { duration: 4000 });
        this.updatingId.set(null);
        this.loadOrders();
        if (this.selectedOrderDetail()?.id === order.id) {
          this.viewDetails(order.id);
        }
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Failed to cancel order';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
        this.updatingId.set(null);
      },
    });
  }

  getStatusBadgeClass(status: OrderStatus): string {
    return statusBadgeClass(status);
  }

  logout(): void {
    this.userContext.logout();
    this.router.navigateByUrl('/');
  }
}
