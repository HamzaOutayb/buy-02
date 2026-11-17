import { DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CartService } from '../../services/cart-service';
import { CreateOrderReq, Order, OrderService } from '../../services/order-service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [DecimalPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './checkout.html',
  styleUrl: './checkout.css',
})
export class Checkout implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);

  readonly items = this.cartService.items;
  readonly subtotal = this.cartService.subtotal;

  readonly isLoadingCart = signal(true);
  readonly cartLoadError = signal('');
  readonly step = signal<1 | 2 | 3>(1);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');
  readonly confirmedOrder = signal<Order | null>(null);

  readonly addressForm = this.fb.nonNullable.group({
    recipientName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [Validators.required, Validators.maxLength(20)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    fullAddress: ['', [Validators.required, Validators.maxLength(300)]],
  });

  ngOnInit(): void {
    this.cartService.loadForUser().subscribe({
      next: () => this.isLoadingCart.set(false),
      error: () => {
        this.cartLoadError.set('Your saved cart could not be loaded. Please try again.');
        this.isLoadingCart.set(false);
      },
    });
  }

  goToReview(): void {
    if (this.isLoadingCart() || this.cartLoadError()) return;
    if (this.items().length === 0) {
      this.errorMessage.set('Your cart is empty. Add products before checking out.');
      return;
    }
    if (this.addressForm.invalid) {
      this.addressForm.markAllAsTouched();
      return;
    }
    this.step.set(2);
  }

  backToAddress(): void {
    this.step.set(1);
  }

  confirmOrder(): void {
    if (this.isSubmitting()) return;
    if (this.isLoadingCart() || this.cartLoadError() || this.items().length === 0) {
      this.errorMessage.set('Your saved cart is empty or unavailable. Please return to your cart.');
      return;
    }

    const values = this.addressForm.getRawValue();
    const req: CreateOrderReq = {
      items: this.items().map((i) => ({ productId: i.productId, quantity: i.quantity })),
      deliveryAddress: {
        recipientName: values.recipientName,
        phone: values.phone,
        city: values.city,
        fullAddress: values.fullAddress,
      },
    };

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.step.set(3);

    this.orderService.createOrder(req).subscribe({
      next: (order) => {
        this.confirmedOrder.set(order);
        this.cartService.clearCart();
        this.isSubmitting.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.errorMessage.set(
          err.error?.message ?? 'Order could not be placed. Please try again.',
        );
        this.isSubmitting.set(false);
      },
    });
  }

  retry(): void {
    this.step.set(2);
    this.errorMessage.set('');
  }
}
