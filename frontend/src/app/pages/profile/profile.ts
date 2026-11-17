import { DecimalPipe } from '@angular/common';
import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { MediaService } from '../../services/media-service';
import { OrderService, SellerSalesAnalytics } from '../../services/order-service';
import { UserContextService } from '../../services/user-context-service';

const MAX_IMAGE_SIZE = 2 * 1024 * 1024;
const ACCEPTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png']);

@Component({
  selector: 'app-profile',
  imports: [DecimalPipe, ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly mediaService = inject(MediaService);
  private readonly orderService = inject(OrderService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly userContext = inject(UserContextService);
  private hasInitializedForm = false;

  readonly user = this.userContext.user;
  readonly analytics = signal<SellerSalesAnalytics | null>(null);
  readonly analyticsError = signal('');
  readonly isLoadingAnalytics = signal(false);
  readonly selectedFile = signal<File | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  readonly profileForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(900)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(999)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(260)]],
  });

  constructor() {
    effect(() => {
      const currentUser = this.user();
      if (currentUser && !this.hasInitializedForm) {
        this.profileForm.patchValue({ name: currentUser.name, email: currentUser.email });
        this.hasInitializedForm = true;
      }
    });
  }

  ngOnInit(): void {
    if (!localStorage.getItem('auth_token')) {
      this.router.navigateByUrl('/login');
      return;
    }
    this.userContext.load().subscribe(() => {
      if (this.user()?.role === 'SELLER') this.loadSellerAnalytics();
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (file && (file.size > MAX_IMAGE_SIZE || !ACCEPTED_IMAGE_TYPES.has(file.type))) {
      input.value = '';
      this.clearSelectedFile();
      this.showError(`${file.name} must be a JPG, JPEG, or PNG image no larger than 2 MB.`);
      return;
    }

    this.clearSelectedFile();
    this.selectedFile.set(file);
    this.previewUrl.set(file ? URL.createObjectURL(file) : null);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  submit(): void {
    if (this.profileForm.invalid || this.isSubmitting()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    const currentUser = this.user();
    const file = this.selectedFile();

    if (file) {
      this.mediaService.uploadImage(file).subscribe({
        next: ({ url }) => this.updateProfile(url),
        error: (error: { error?: { message?: string } }) => this.finishWithError(
          error.error?.message ?? 'The avatar could not be uploaded. Please try again.',
          true,
        ),
      });
      return;
    }

    this.updateProfile(currentUser?.avatar ?? null);
  }

  private updateProfile(avatar: string | null): void {
    const values = this.profileForm.getRawValue();
    this.userContext.updateProfile({ ...values, avatar }).subscribe({
      next: () => {
        this.profileForm.controls.password.reset();
        this.clearSelectedFile();
        this.successMessage.set('Your profile has been updated.');
        this.isSubmitting.set(false);
      },
      error: (error: { error?: { message?: string } }) => this.finishWithError(
        error.error?.message ?? 'Your profile could not be updated. Check your password and try again.',
      ),
    });
  }

  private loadSellerAnalytics(): void {
    this.isLoadingAnalytics.set(true);
    this.orderService.getSellerAnalytics().subscribe({
      next: (analytics) => {
        this.analytics.set(analytics);
        this.isLoadingAnalytics.set(false);
      },
      error: () => {
        this.analyticsError.set('Sales analytics could not be loaded.');
        this.isLoadingAnalytics.set(false);
      },
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

  private clearSelectedFile(): void {
    const previewUrl = this.previewUrl();
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    this.selectedFile.set(null);
    this.previewUrl.set(null);
  }
}
