import { DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../services/cart-service';
import { UserContextService } from '../../services/user-context-service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.css',
})
export class Cart {
  readonly cartService = inject(CartService);
  private readonly userContext = inject(UserContextService);
  private readonly router = inject(Router);

  readonly items = this.cartService.items;
  readonly count = this.cartService.count;
  readonly subtotal = this.cartService.subtotal;
  readonly user = this.userContext.user;

  decreaseQuantity(productId: string, currentQty: number): void {
    if (currentQty <= 1) {
      this.cartService.removeFromCart(productId);
    } else {
      this.cartService.updateQuantity(productId, currentQty - 1);
    }
  }

  increaseQuantity(productId: string, currentQty: number, maxQty: number): void {
    if (currentQty < maxQty) {
      this.cartService.updateQuantity(productId, currentQty + 1);
    }
  }

  removeItem(productId: string): void {
    this.cartService.removeFromCart(productId);
  }

  proceedToCheckout(): void {
    if (!localStorage.getItem('auth_token')) {
      this.router.navigateByUrl('/login');
      return;
    }
    this.router.navigateByUrl('/checkout');
  }
}
