import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { Product, ProductInput } from '../../models/product.model';
import { MediaService } from '../../services/media.service';
import { ProductService } from '../../services/product.service';
import { UserService } from '../../services/user.service';

interface SelectedImage {
  file: File;
  previewUrl: string;
}

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.css'
})
export class ProductFormComponent implements OnInit, OnDestroy {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly productService = inject(ProductService);
  private readonly mediaService = inject(MediaService);
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly maximumImages = 5;
  readonly maximumImageSize = 2 * 1024 * 1024;
  readonly maximumTotalImageSize = 7 * 1024 * 1024;

readonly form = this.formBuilder.group({
  name: [
    '',
    [
      Validators.required,
      Validators.maxLength(50)
    ]
  ],

  description: [
    '',
    Validators.maxLength(2000)
  ],

  price: [
    0,
    [
      Validators.required,
      Validators.min(0.01),
      Validators.max(100000000),
    ]
  ],

  quantity: [
    0,
    [
      Validators.required,
      Validators.min(0),
      Validators.max(100000000),
    ]
  ]
});

  productId = '';
  isEditing = false;
  isProductReady = true;
  isLoadingProduct = false;
  isSaving = false;
  errorMessage = '';
  warningMessage = '';
  imageError = '';
  existingImageUrls: string[] = [];
  selectedImages: SelectedImage[] = [];
  private originalImageUrls: string[] = [];

  ngOnInit(): void
  {
    const productId = this.route.snapshot.paramMap.get('id');

    if (!productId) {
      return;
    }

    this.productId = productId;
    this.isEditing = true;
    this.isProductReady = false;
    this.isLoadingProduct = true;
    this.loadProduct(productId);
  }

  ngOnDestroy(): void
  {
    for (const image of this.selectedImages) {
      URL.revokeObjectURL(image.previewUrl);
    }
  }

  onImagesSelected(event: Event): void
  {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const files = Array.from(input.files);
    const imageCount =
      this.existingImageUrls.length + this.selectedImages.length;

    if (imageCount + files.length > this.maximumImages) {
      this.imageError = 'You can select up to five images for a product.';
      input.value = '';
      return;
    }

    for (const file of files) {
      if (!file.type.startsWith('image/')) {
        this.imageError = 'Choose image files only.';
        input.value = '';
        return;
      }

      if (file.size > this.maximumImageSize) {
        this.imageError = 'Each image must be 2 MB or smaller.';
        input.value = '';
        return;
      }
    }

    const totalImageSize =
      this.selectedImages.reduce((total, image) => total + image.file.size, 0) +
      files.reduce((total, file) => total + file.size, 0);
    if (totalImageSize > this.maximumTotalImageSize) {
      this.imageError = 'Combined image size must be 7 MB or smaller per upload.';
      input.value = '';
      return;
    }

    this.imageError = '';

    for (const file of files) {
      this.selectedImages.push({
        file,
        previewUrl: URL.createObjectURL(file)
      });
    }

    input.value = '';
  }

  removeExistingImage(index: number): void
  {
    this.existingImageUrls.splice(index, 1);
    this.imageError = '';
  }

  removeSelectedImage(index: number): void
  {
    const image = this.selectedImages[index];
    URL.revokeObjectURL(image.previewUrl);
    this.selectedImages.splice(index, 1);
    this.imageError = '';
  }

  submit(): void
  {
    this.errorMessage = '';
    this.warningMessage = '';
    this.form.markAllAsTouched();

    if (this.form.invalid || this.imageError) {
      return;
    }

    this.isSaving = true;

    const values = this.form.getRawValue();
    const product: ProductInput = {
      name: values.name.trim(),
      description: values.description.trim(),
      price: values.price,
      quantity: values.quantity,
      imageUrls: []
    };
    const files: File[] = [];

    for (const image of this.selectedImages) {
      files.push(image.file);
    }

    if (files.length === 0) {
      this.saveProduct(product, [...this.existingImageUrls], []);
      return;
    }

    this.mediaService.uploadImages(files).subscribe({
      next: (uploadedImageUrls) => {
        const imageUrls = [...this.existingImageUrls, ...uploadedImageUrls];
        this.saveProduct(product, imageUrls, uploadedImageUrls);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'The images could not be uploaded. Check them and try again.'
        );
        this.isSaving = false;
      }
    });
  }

  private loadProduct(productId: string): void
  {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.productService.getProduct(productId).subscribe({
          next: (product) => {
            if (product.sellerId !== user.id) {
              this.errorMessage = 'You can only edit your own products.';
              this.isLoadingProduct = false;
              return;
            }

            this.populateForm(product);
            this.isProductReady = true;
            this.isLoadingProduct = false;
          },
          error: (error: HttpErrorResponse) => {
            this.errorMessage = getApiErrorMessage(
              error,
              'This product could not be loaded.'
            );
            this.isLoadingProduct = false;
          }
        });
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'Your account information could not be loaded.'
        );
        this.isLoadingProduct = false;
      }
    });
  }

  private populateForm(product: Product): void
  {
    let description = '';

    if (product.description) {
      description = product.description;
    }

    this.form.patchValue({
      name: product.name,
      description,
      price: product.price,
      quantity: product.quantity
    });

    this.originalImageUrls = [];

    if (product.imageUrls) {
      this.originalImageUrls = [...product.imageUrls];
    }

    this.existingImageUrls = [...this.originalImageUrls];
  }

  private saveProduct(
    product: ProductInput,
    imageUrls: string[],
    uploadedImageUrls: string[]
  ): void
  {
    const productWithImages: ProductInput = {
      ...product,
      imageUrls
    };

    if (this.isEditing) {
      this.updateProduct(productWithImages, uploadedImageUrls);
      return;
    }

    this.createProduct(productWithImages, uploadedImageUrls);
  }

  private createProduct(
    product: ProductInput,
    uploadedImageUrls: string[]
  ): void
  {
    this.productService.createProduct(product).subscribe({
      next: () => {
        this.router.navigateByUrl('/seller/products');
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'The product could not be created.'
        );
        this.cleanupUploadedImages(uploadedImageUrls);
        this.isSaving = false;
      }
    });
  }

  private updateProduct(
    product: ProductInput,
    uploadedImageUrls: string[]
  ): void
  {
    this.productService.updateProduct(this.productId, product).subscribe({
      next: () => {
        this.deleteRemovedImages(product.imageUrls);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'The product could not be updated.'
        );
        this.cleanupUploadedImages(uploadedImageUrls);
        this.isSaving = false;
      }
    });
  }

  private deleteRemovedImages(savedImageUrls: string[]): void
  {
    const removedImageUrls = this.originalImageUrls.filter(
      (imageUrl) => !savedImageUrls.includes(imageUrl)
    );
    this.originalImageUrls = [...savedImageUrls];

    if (removedImageUrls.length === 0) {
      this.router.navigateByUrl('/seller/products');
      return;
    }

    this.mediaService.deleteImages(removedImageUrls).subscribe({
      next: () => {
        this.router.navigateByUrl('/seller/products');
      },
      error: () => {
        this.warningMessage =
          'The product was saved, but some removed images could not be deleted.';
        this.isSaving = false;
      }
    });
  }

  private cleanupUploadedImages(imageUrls: string[]): void
  {
    if (imageUrls.length === 0) {
      return;
    }

    this.mediaService.deleteImages(imageUrls).subscribe({
      error: () => {
        this.errorMessage += ' Some uploaded images could not be cleaned up.';
      }
    });
  }
}
