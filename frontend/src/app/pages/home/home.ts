import { Component, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe, SlicePipe, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PRODUCT_CATEGORIES, Product, ProductService } from '../../services/product-service';
import { UserContextService } from '../../services/user-context-service';

@Component({
  selector: 'app-home',
  imports: [DecimalPipe, FormsModule, RouterLink, SlicePipe, TitleCasePipe],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly userContext = inject(UserContextService);
  readonly user = this.userContext.user;
  readonly userLoading = this.userContext.isLoading;
  readonly products = signal<Product[]>([]);
  readonly isLoading = signal(true);
  readonly hasError = signal(false);
  readonly productCategories = PRODUCT_CATEGORIES;
  keyword = '';
  selectedCategories: string[] = [];
  showCategoryFilters = false;

  ngOnInit(): void {
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  searchProducts(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.productService.searchProducts(this.keyword, this.selectedCategories).subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  isCategorySelected(category: string): boolean {
    return this.selectedCategories.includes(category);
  }

  toggleCategory(category: string): void {
    this.selectedCategories = this.isCategorySelected(category)
      ? this.selectedCategories.filter((selectedCategory) => selectedCategory !== category)
      : [...this.selectedCategories, category];
  }

  toggleCategoryFilters(): void {
    this.showCategoryFilters = !this.showCategoryFilters;
  }

  imageFor(product: Product): string {
    return product.imageUrls[0];
  }

  logout(): void {
    this.userContext.logout();
  }
}
