import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { Cart, CartItem } from '../../models/order.model';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css'
})
export class CartComponent implements OnInit {
  private readonly cartService = inject(CartService);

  cart: Cart = { items: [], subtotal: 0 };
  isLoading = true;
  errorMessage = '';
  updatingProductId = '';

  ngOnInit(): void
  {
    this.loadCart();
  }

  loadCart(): void
  {
    this.isLoading = true;
    this.errorMessage = '';
    this.cartService.getCart().subscribe({
      next: (cart) => {
        this.cart = cart;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'Your cart could not be loaded.');
        this.isLoading = false;
      }
    });
  }

  changeQuantity(item: CartItem, event: Event): void
  {
    const input = event.target as HTMLInputElement;
    const quantity = Number(input.value);
    if (!Number.isInteger(quantity) || quantity < 1) {
      input.value = String(item.quantity);
      return;
    }

    this.updatingProductId = item.productId;
    this.cartService.updateQuantity(item.productId, quantity).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.updatingProductId = '';
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'The quantity could not be updated.');
        this.updatingProductId = '';
        this.loadCart();
      }
    });
  }

  remove(item: CartItem): void
  {
    this.updatingProductId = item.productId;
    this.cartService.removeItem(item.productId).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.updatingProductId = '';
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(error, 'The item could not be removed.');
        this.updatingProductId = '';
      }
    });
  }
}
