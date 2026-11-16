import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  OrderPage,
  OrderService,
  OrderStatus,
  OrderSummary,
  statusBadgeClass,
} from '../../services/order-service';
import { UserContextService } from '../../services/user-context-service';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, NgClass, RouterLink],
  templateUrl: './orders.html',
  styleUrl: './orders.css',
})
export class Orders implements OnInit {
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
  readonly cancellingId = signal<string | null>(null);
  readonly reorderingId = signal<string | null>(null);

  // Filters
  selectedStatus: OrderStatus | '' = '';
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
    if (this.searchReference.trim()) {
      params.orderReference = this.searchReference.trim();
    }
    if (this.dateFrom) {
      params.dateFrom = new Date(this.dateFrom).toISOString();
    }
    if (this.dateTo) {
      // Include end of day
      const d = new Date(this.dateTo);
      d.setHours(23, 59, 59, 999);
      params.dateTo = d.toISOString();
    }

    this.orderService.getMyOrders(params).subscribe({
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

  cancelOrder(order: OrderSummary): void {
    if (!window.confirm(`Are you sure you want to cancel order #${order.orderReference}?`)) {
      return;
    }

    this.cancellingId.set(order.id);
    this.orderService.cancelOrder(order.id).subscribe({
      next: () => {
        this.snackBar.open(`Order #${order.orderReference} cancelled`, 'Dismiss', { duration: 4000 });
        this.cancellingId.set(null);
        this.loadOrders();
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Failed to cancel order';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
        this.cancellingId.set(null);
      },
    });
  }

  reorder(order: OrderSummary): void {
    if (!window.confirm(`Reorder items from #${order.orderReference}?`)) {
      return;
    }

    this.reorderingId.set(order.id);
    this.orderService.reorder(order.id).subscribe({
      next: (newOrder) => {
        this.snackBar.open(
          `Reorder successful! New order: #${newOrder.orderReference}`,
          'View',
          { duration: 5000 }
        );
        this.reorderingId.set(null);
        this.router.navigate(['/orders', newOrder.id]);
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Reorder failed. Products may be out of stock.';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
        this.reorderingId.set(null);
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
