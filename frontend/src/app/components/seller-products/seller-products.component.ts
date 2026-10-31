import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { Product } from '../../models/product.model';
import { ProductService } from '../../services/product.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-seller-products',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './seller-products.component.html',
  styleUrl: './seller-products.component.css'
})
export class SellerProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly userService = inject(UserService);

  products: Product[] = [];
  sellerId = '';
  isLoading = true;
  deletingProductId = '';
  errorMessage = '';
  totalUnits = 0;
  availableCount = 0;
  searchTerm = '';
  category = '';

  get filteredProducts(): Product[]
  {
    const searchTerm = this.searchTerm.trim().toLowerCase();
    const category = this.category.trim().toLowerCase();

    return this.products.filter((product) =>
      (!searchTerm || product.name.toLowerCase().includes(searchTerm)) &&
      (!category || (product.category ?? '').trim().toLowerCase() === category)
    );
  }

  clearFilters(): void
  {
    this.searchTerm = '';
    this.category = '';
  }

  ngOnInit(): void
  {
    this.loadSellerProducts();
  }

  loadSellerProducts(): void
  {
    this.errorMessage = '';

    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.sellerId = user.id;
        this.loadProducts();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'Your products could not be loaded.'
        );
        this.isLoading = false;
      }
    });
  }

  deleteProduct(product: Product): void
  {
    const confirmed = window.confirm(
      `Delete "${product.name}"? This cannot be undone.`
    );

    if (!confirmed) {
      return;
    }

    this.deletingProductId = product.id;
    this.errorMessage = '';

    this.productService.deleteProduct(product.id).subscribe({
      next: () => {
        this.products = this.products.filter(
          (item) => item.id !== product.id
        );
        this.updateOverview();
        this.deletingProductId = '';
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'This product could not be deleted.'
        );
        this.deletingProductId = '';
      }
    });
  }

  reload(): void
  {
    this.isLoading = true;
    this.loadSellerProducts();
  }

  private loadProducts(): void
  {
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products = products.filter(
          (product) => product.sellerId === this.sellerId
        );
        this.updateOverview();
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'Your products could not be loaded.'
        );
        this.isLoading = false;
      }
    });
  }

  private updateOverview(): void
  {
    this.totalUnits = 0;
    this.availableCount = 0;

    for (const product of this.products) {
      this.totalUnits += product.quantity;

      if (product.quantity > 0) {
        this.availableCount += 1;
      }
    }
  }
}
