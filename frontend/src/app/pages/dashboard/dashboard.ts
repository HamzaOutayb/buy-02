import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { Component, ElementRef, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MediaService } from '../../services/media-service';
import {
  PRODUCT_CATEGORIES,
  Product,
  ProductRequest,
  ProductService,
} from '../../services/product-service';
import { OrderService, SellerSalesAnalytics } from '../../services/order-service';

interface ImagePreview {
  file: File;
  url: string;
}

const MAX_IMAGE_SIZE = 2 * 1024 * 1024;
const MAX_PRODUCT_IMAGES = 5;
const ACCEPTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png']);

@Component({
  selector: 'app-dashboard',
  imports: [DecimalPipe, ReactiveFormsModule, TitleCasePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnDestroy, OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly mediaService = inject(MediaService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly productService = inject(ProductService);
  private readonly orderService = inject(OrderService);

  @ViewChild('productImagesInput') private productImagesInput?: ElementRef<HTMLInputElement>;

  readonly products = signal<Product[]>([]);
  readonly analytics = signal<SellerSalesAnalytics | null>(null);
  readonly analyticsError = signal('');
  readonly selectedFiles = signal<File[]>([]);
  readonly imagePreviews = signal<ImagePreview[]>([]);
  readonly existingImageUrls = signal<string[]>([]);
  readonly editingProduct = signal<Product | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly productCategories = PRODUCT_CATEGORIES;

  readonly productForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(500)]],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    price: [0, [Validators.required, Validators.min(0.1)]],
    quantity: [0, [Validators.required, Validators.min(0)]],
    categories: [[] as string[]],
  });

  ngOnInit(): void {
    this.loadProducts();
    this.orderService.getSellerAnalytics().subscribe({
      next: (analytics) => this.analytics.set(analytics),
      error: () => this.analyticsError.set('Sales analytics could not be loaded.'),
    });
  }

  ngOnDestroy(): void {
    this.revokePreviewUrls();
  }

  loadProducts(): void {
    this.isLoading.set(true);
    this.productService.getMyProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(
          'Your products could not be loaded. Make sure you are signed in as a seller.',
        );
        this.isLoading.set(false);
      },
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    const invalidFile = files.find(
      (file) => file.size > MAX_IMAGE_SIZE || !ACCEPTED_IMAGE_TYPES.has(file.type),
    );

    if (invalidFile) {
      this.setSelectedFiles([]);
      input.value = '';
      this.showError(
        `${invalidFile.name} must be a JPG, JPEG, or PNG image no larger than 2 MB.`,
      );
      return;
    }

    if (files.length + this.existingImageUrls().length > MAX_PRODUCT_IMAGES) {
      this.setSelectedFiles([]);
      input.value = '';
      this.showError('A product can have a maximum of 5 images.');
      return;
    }

    this.errorMessage.set('');
    this.setSelectedFiles(files);
  }

  removeSelectedFile(index: number): void {
    const files = this.selectedFiles().filter((_, fileIndex) => fileIndex !== index);
    this.setSelectedFiles(files);
    if (this.productImagesInput) {
      this.productImagesInput.nativeElement.value = '';
    }
  }

  removeExistingImage(index: number): void {
    this.existingImageUrls.update((imageUrls) =>
      imageUrls.filter((_, imageIndex) => imageIndex !== index),
    );
  }

  isCategorySelected(category: string): boolean {
    return this.productForm.controls.categories.value.includes(category);
  }

  toggleCategory(category: string): void {
    const selectedCategories = this.productForm.controls.categories.value;
    const categories = selectedCategories.includes(category)
      ? selectedCategories.filter((selectedCategory) => selectedCategory !== category)
      : [...selectedCategories, category];
    this.productForm.controls.categories.setValue(categories);
  }

  editProduct(product: Product): void {
    this.editingProduct.set(product);
    this.productForm.setValue({
      name: product.name,
      description: product.description,
      price: product.price,
      quantity: product.quantity,
      categories: product.categories,
    });
    this.clearSelectedFiles();
    this.existingImageUrls.set([...product.imageUrls]);
    this.errorMessage.set('');
    this.successMessage.set('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.editingProduct.set(null);
    this.productForm.reset({ name: '', description: '', price: 0, quantity: 0, categories: [] });
    this.clearSelectedFiles();
    this.existingImageUrls.set([]);
    this.errorMessage.set('');
  }

  submitProduct(): void {
    if (this.productForm.invalid || this.isSubmitting()) {
      this.productForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    const product = this.editingProduct();
    const files = this.selectedFiles();
    const existingImageUrls = this.existingImageUrls();
    if (files.length + existingImageUrls.length > MAX_PRODUCT_IMAGES) {
      this.showError('A product can have a maximum of 5 images.');
      this.isSubmitting.set(false);
      return;
    }
    const upload$ = files.length ? this.mediaService.uploadImages(files) : null;

    if (upload$) {
      upload$.subscribe({
        next: ({ urls }) => this.saveProduct({ imageUrls: [...existingImageUrls, ...urls] }, product),
        error: (error: { error?: { message?: string } }) => this.finishWithError(
          error.error?.message ?? 'Images could not be uploaded. Please try again.',
          true,
        ),
      });
      return;
    }

    this.saveProduct({ imageUrls: existingImageUrls }, product);
  }

  deleteProduct(product: Product): void {
    if (!window.confirm(`Delete ${product.name}? This cannot be undone.`)) {
      return;
    }

    this.errorMessage.set('');
    this.productService.deleteProduct(product.id).subscribe({
      next: () => {
        this.products.update((products) => products.filter((item) => item.id !== product.id));
        if (this.editingProduct()?.id === product.id) {
          this.cancelEdit();
        }
      },
      error: (error: { status?: number; error?: { message?: string } }) => {
        const message = error.status === 403
          ? 'You are not allowed to delete this product.'
          : error.error?.message ?? 'This product could not be deleted. Please try again.';
        this.showError(message);
      },
    });
  }

  private saveProduct(upload: { imageUrls: string[] }, product: Product | null): void {
    const values = this.productForm.getRawValue();
    const request: ProductRequest = { ...values, imageUrls: upload.imageUrls };
    const save$ = product
      ? this.productService.updateProduct(product.id, request)
      : this.productService.createProduct(request);

    save$.subscribe({
      next: (savedProduct) => {
        this.products.update((products) =>
          product
            ? products.map((item) => (item.id === savedProduct.id ? savedProduct : item))
            : [savedProduct, ...products],
        );
        this.successMessage.set(product ? 'Product updated.' : 'Product created.');
        this.cancelEdit();
        this.isSubmitting.set(false);
      },
      error: () =>
        this.finishWithError(
          'The product could not be saved. Please check the details and try again.',
        ),
    });
  }

  private finishWithError(message: string, showToast = true): void {
    if (showToast) {
      this.snackBar.open(message, 'Dismiss', { duration: 5000, panelClass: ['error-snackbar'] });
    }
    this.errorMessage.set(message);
    this.isSubmitting.set(false);
  }

  private showError(message: string): void {
    this.errorMessage.set(message);
    this.snackBar.open(message, 'Dismiss', { duration: 5000, panelClass: ['error-snackbar'] });
  }

  private setSelectedFiles(files: File[]): void {
    this.revokePreviewUrls();
    this.selectedFiles.set(files);
    this.imagePreviews.set(files.map((file) => ({ file, url: URL.createObjectURL(file) })));
  }

  private clearSelectedFiles(): void {
    this.setSelectedFiles([]);
    if (this.productImagesInput) {
      this.productImagesInput.nativeElement.value = '';
    }
  }

  private revokePreviewUrls(): void {
    this.imagePreviews().forEach(({ url }) => URL.revokeObjectURL(url));
    this.imagePreviews.set([]);
  }
}
