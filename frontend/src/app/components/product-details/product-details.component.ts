import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { Product } from '../../models/product.model';
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
  private readonly route = inject(ActivatedRoute);

  product: Product | null = null;
  imageUrls: string[] = [];
  activeImageIndex = 0;
  isLoading = true;
  errorMessage = '';

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
