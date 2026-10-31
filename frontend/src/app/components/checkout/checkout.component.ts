import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { Cart } from '../../models/order.model';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [DecimalPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent implements OnInit {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);

  readonly form = this.formBuilder.group({
    recipientName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [Validators.required, Validators.maxLength(30)]],
    street: ['', [Validators.required, Validators.maxLength(200)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    postalCode: ['', [Validators.required, Validators.maxLength(20)]]
  });

  cart: Cart = { items: [], subtotal: 0 };
  isLoading = true;
  isSubmitting = false;
  errorMessage = '';
  cartLoadError = '';
  private readonly checkoutKey = crypto.randomUUID();

  ngOnInit(): void
  {
    this.loadCart();
  }

  loadCart(): void
  {
    this.isLoading = true;
    this.cartLoadError = '';
    this.cartService.getCart().subscribe({
      next: (cart) => {
        this.cart = cart;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.cartLoadError = getApiErrorMessage(error, 'Your cart could not be loaded.');
        this.isLoading = false;
      }
    });
  }

  submit(): void
  {
    this.errorMessage = '';
    this.form.markAllAsTouched();
    if (this.form.invalid || this.cart.items.length === 0) {
      return;
    }

    this.isSubmitting = true;
    this.orderService.checkout({
      checkoutKey: this.checkoutKey,
      deliveryAddress: this.form.getRawValue()
    }).subscribe({
      next: () => void this.router.navigateByUrl('/orders'),
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'Checkout could not be completed.');
        this.isSubmitting = false;
      }
    });
  }
}
