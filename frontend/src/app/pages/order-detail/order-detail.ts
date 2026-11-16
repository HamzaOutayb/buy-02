import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  Order,
  OrderService,
  OrderStatus,
  statusBadgeClass,
} from '../../services/order-service';
import { UserContextService } from '../../services/user-context-service';

interface TimelineStep {
  label: string;
  sublabel: string;
  status: OrderStatus;
}

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [DatePipe, DecimalPipe, NgClass, RouterLink],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.css',
})
export class OrderDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly userContext = inject(UserContextService);
  private readonly snackBar = inject(MatSnackBar);

  readonly user = this.userContext.user;
  readonly order = signal<Order | null>(null);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly isCancelling = signal(false);
  readonly isReordering = signal(false);

  readonly timelineSteps: TimelineStep[] = [
    { label: 'Order Placed', sublabel: 'Pending verification', status: 'PENDING' },
    { label: 'Confirmed', sublabel: 'Order confirmed by seller', status: 'CONFIRMED' },
    { label: 'Processing', sublabel: 'Being prepared for shipment', status: 'PROCESSING' },
    { label: 'Shipped', sublabel: 'In transit to delivery address', status: 'SHIPPED' },
    { label: 'Delivered', sublabel: 'Package received by buyer', status: 'DELIVERED' },
  ];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.hasError.set(true);
      this.isLoading.set(false);
      return;
    }
    this.loadOrder(id);
  }

  loadOrder(id: string): void {
    this.isLoading.set(true);
    this.hasError.set(false);
    this.orderService.getOrderById(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  getCurrentStepIndex(): number {
    const status = this.order()?.status;
    if (!status || status === 'CANCELLED') return -1;
    switch (status) {
      case 'PENDING':
        return 0;
      case 'CONFIRMED':
        return 1;
      case 'PROCESSING':
        return 2;
      case 'SHIPPED':
        return 3;
      case 'DELIVERED':
        return 4;
      default:
        return 0;
    }
  }

  isStepCompleted(stepIndex: number): boolean {
    const currentIndex = this.getCurrentStepIndex();
    return currentIndex >= 0 && stepIndex < currentIndex;
  }

  isStepCurrent(stepIndex: number): boolean {
    return stepIndex === this.getCurrentStepIndex();
  }

  cancelOrder(): void {
    const currentOrder = this.order();
    if (!currentOrder) return;
    if (!window.confirm(`Are you sure you want to cancel order #${currentOrder.orderReference}?`)) {
      return;
    }

    this.isCancelling.set(true);
    this.orderService.cancelOrder(currentOrder.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.snackBar.open('Order cancelled successfully', 'Dismiss', { duration: 4000 });
        this.isCancelling.set(false);
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Failed to cancel order';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
        this.isCancelling.set(false);
      },
    });
  }

  reorder(): void {
    const currentOrder = this.order();
    if (!currentOrder) return;
    if (!window.confirm(`Reorder items from #${currentOrder.orderReference}?`)) {
      return;
    }

    this.isReordering.set(true);
    this.orderService.reorder(currentOrder.id).subscribe({
      next: (newOrder) => {
        this.snackBar.open(
          `Reorder successful! Redirecting to new order #${newOrder.orderReference}...`,
          'View',
          { duration: 4000 }
        );
        this.isReordering.set(false);
        this.router.navigate(['/orders', newOrder.id]);
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Reorder failed. Products may be out of stock.';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
        this.isReordering.set(false);
      },
    });
  }

  getStatusBadgeClass(status: OrderStatus): string {
    return statusBadgeClass(status);
  }
}
