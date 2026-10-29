import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { Product } from '../../models/product.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './product-details.component.html',
  styleUrl: './product-details.component.css'
})
export class ProductDetailsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  product: Product | null = null;
  imageUrls: string[] = [];
  activeImageIndex = 0;
  isLoading = true;
  errorMessage = '';
  cartMessage = '';
  isAddingToCart = false;

  get authenticated(): boolean
  {
    return this.authService.isAuthenticated();
  }

  get canAddToCart(): boolean
  {
    return this.authenticated && this.authService.isClient();
  }

  ngOnInit(): void
  {
    const productId = this.route.snapshot.paramMap.get('id');

    if (!productId) {
      this.errorMessage = 'This product could not be found.';
      this.isLoading = false;
      return;
    }

    this.loadProduct(productId);
  }

  selectImage(index: number): void
  {
    this.activeImageIndex = index;
  }

  showPreviousImage(): void
  {
    if (this.imageUrls.length < 2) {
      return;
    }

    if (this.activeImageIndex === 0) {
      this.activeImageIndex = this.imageUrls.length - 1;
      return;
    }

    this.activeImageIndex -= 1;
  }

  showNextImage(): void
  {
    if (this.imageUrls.length < 2) {
      return;
    }

    if (this.activeImageIndex === this.imageUrls.length - 1) {
      this.activeImageIndex = 0;
      return;
    }

    this.activeImageIndex += 1;
  }

  addToCart(): void
  {
    this.cartMessage = '';
    if (!this.authService.isAuthenticated()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (!this.canAddToCart || !this.product || this.product.quantity < 1) {
      this.cartMessage = 'Only clients can add available products to a cart.';
      return;
    }
    this.isAddingToCart = true;
    this.cartService.addItem(this.product.id).subscribe({
      next: () => {
        this.cartMessage = 'Added to your cart.';
        this.isAddingToCart = false;
      },
      error: (error: HttpErrorResponse) => {
        this.cartMessage = getApiErrorMessage(error, 'This product could not be added to your cart.');
        this.isAddingToCart = false;
      }
    });
  }

  private loadProduct(productId: string): void
  {
    this.productService.getProduct(productId).subscribe({
      next: (product) => {
        this.product = product;
        this.imageUrls = [];

        if (product.imageUrls) {
          this.imageUrls = [...product.imageUrls];
        }

        this.activeImageIndex = 0;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'This product could not be loaded.'
        );
        this.isLoading = false;
      }
    });
  }
}
