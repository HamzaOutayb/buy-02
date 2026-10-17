import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { RegisterRequest, UserRole } from '../../models/auth.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  readonly UserRole = UserRole;

  readonly form = this.formBuilder.group({
    name: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(100)
      ]
    ],
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.maxLength(254)
      ]
    ],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(72)
      ]
    ],
    role: [UserRole.Client, Validators.required]
  });

  errorMessage = '';
  successMessage = '';
  isSubmitting = false;

  submit(): void
  {
    this.errorMessage = '';
    this.successMessage = '';

    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    this.isSubmitting = true;

    const values = this.form.getRawValue();
    const request: RegisterRequest = {
      name: values.name.trim(),
      email: values.email.trim(),
      password: values.password,
      role: values.role
    };

    this.authService.register(request).subscribe({
      next: () => {
        this.successMessage = 'Account created. You can now sign in.';
        this.isSubmitting = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'Could not create the account. Check your details and try again.'
        );
        this.isSubmitting = false;
      }
    });
  }
}
