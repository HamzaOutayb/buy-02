import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Product } from '../../models/product.model';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './products.component.html',
  styleUrl: './products.component.css'
})
export class ProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);

  products: Product[] = [];
  isLoading = true;
  hasError = false;
  searchTerm = '';
  category = '';
  page = 0;
  readonly pageSize = 12;
  totalElements = 0;
  totalPages = 0;
  filterError = '';

  get hasActiveFilters(): boolean
  {
    return Boolean(
      this.searchTerm.trim() ||
      this.category.trim()
    );
  }

  ngOnInit(): void
  {
    this.loadProducts();
  }

  loadProducts(): void
  {
    this.isLoading = true;
    this.hasError = false;
    this.filterError = '';

    this.productService.searchProducts({
      q: this.searchTerm.trim(),
      category: this.category.trim(),
      page: this.page,
      size: this.pageSize
    }).subscribe({
      next: (result) => {
        this.products = result.content;
        this.totalElements = result.totalElements;
        this.totalPages = result.totalPages;
        this.isLoading = false;
      },
      error: () => {
        this.hasError = true;
        this.products = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.isLoading = false;
      }
    });
  }

  applyFilters(): void
  {
    this.page = 0;
    this.loadProducts();
  }

  clearFilters(): void
  {
    this.searchTerm = '';
    this.category = '';
    this.page = 0;
    this.loadProducts();
  }

  goToPage(page: number): void
  {
    if (page < 0 || page >= this.totalPages || page === this.page) {
      return;
    }

    this.page = page;
    this.loadProducts();
    document.getElementById('product-grid')?.scrollIntoView({ behavior: 'smooth' });
  }
}
