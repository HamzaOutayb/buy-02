import { DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Product, ProductService } from '../../services/product-service';
import { UserContextService } from '../../services/user-context-service';
import { CartService } from '../../services/cart-service';

@Component({
  selector: 'app-product-detail',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
})
export class ProductDetail implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly userContext = inject(UserContextService);
  private readonly cartService = inject(CartService);
  private readonly snackBar = inject(MatSnackBar);

  readonly user = this.userContext.user;
  readonly product = signal<Product | null>(null);
  readonly selectedImage = signal('');
  readonly selectedQuantity = signal(1);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);

  ngOnInit(): void {
    const productId = this.route.snapshot.paramMap.get('id');
    if (!productId) {
      this.isLoading.set(false);
      this.hasError.set(true);
      return;
    }

    this.productService.getProduct(productId).subscribe({
      next: (product) => {
        this.product.set(product);
        this.selectedImage.set(product.imageUrls[0] ?? '');
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  selectImage(imageUrl: string): void {
    this.selectedImage.set(imageUrl);
  }

  increaseQuantity(max: number): void {
    if (this.selectedQuantity() < max) {
      this.selectedQuantity.update((q) => q + 1);
    }
  }

  decreaseQuantity(): void {
    if (this.selectedQuantity() > 1) {
      this.selectedQuantity.update((q) => q - 1);
    }
  }

  addToCart(product: Product): void {
    if (this.user()?.role !== 'CLIENT') {
      this.router.navigateByUrl('/login');
      return;
    }

    const qty = this.selectedQuantity();
    const imageUrl = product.imageUrls && product.imageUrls.length > 0 ? product.imageUrls[0] : '';

    this.cartService.addToCart({
      productId: product.id,
      productName: product.name,
      price: product.price,
      quantity: qty,
      imageUrl,
      sellerId: product.userId,
      maxQuantity: product.quantity,
    });

    const ref = this.snackBar.open(`Added ${qty} item(s) to your cart!`, 'View Cart', {
      duration: 4000,
    });
    ref.onAction().subscribe(() => {
      this.router.navigateByUrl('/cart');
    });
  }
}
