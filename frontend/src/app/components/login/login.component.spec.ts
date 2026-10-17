/// <reference types="jasmine" />

import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login', 'saveToken']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  // Keeps login disabled until email and password are entered.
  it('starts with required fields invalid', () => {
    expect(component.form.invalid).toBeTrue();
    expect(component.form.controls.email.hasError('required')).toBeTrue();
    expect(component.form.controls.password.hasError('required')).toBeTrue();
  });

  it('shows email validation feedback for malformed addresses', () => {
    const invalidEmails = [
      'not-an-email',
      'alice@',
      '@example.com',
      'alice..smith@example.com',
      `${'a'.repeat(65)}@example.com`
    ];

    for (const email of invalidEmails) {
      component.form.controls.email.setValue(email);
      expect(component.form.controls.email.hasError('email')).withContext(email).toBeTrue();
    }

    component.form.controls.email.markAsTouched();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#email-error')?.textContent)
      .toContain('Enter a valid email address.');
    expect(fixture.nativeElement.querySelector('#email')?.getAttribute('aria-invalid'))
      .toBe('true');
  });

  it('accepts a syntactically valid email address', () => {
    component.form.controls.email.setValue('alice+shop@example.co.uk');

    expect(component.form.controls.email.valid).toBeTrue();
  });

  // Marks missing fields and does not send an invalid login request.
  it('does not submit an invalid form', () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(component.form.controls.email.touched).toBeTrue();
    expect(component.form.controls.password.touched).toBeTrue();
    expect(fixture.nativeElement.querySelectorAll('[role="alert"]').length).toBe(2);
    expect(authService.login).not.toHaveBeenCalled();
  });

  // Submits valid credentials through the rendered form and navigates to products.
  it('logs in and navigates after a successful response', () => {
    const emailInput: HTMLInputElement = fixture.nativeElement.querySelector('#email');
    const passwordInput: HTMLInputElement = fixture.nativeElement.querySelector('#password');
    emailInput.value = 'alice@example.com';
    emailInput.dispatchEvent(new Event('input'));
    passwordInput.value = 'password123';
    passwordInput.dispatchEvent(new Event('input'));
    authService.login.and.returnValue(of({
      token: 'test-token',
      email: 'alice@example.com',
      role: 'CLIENT' as never
    }));
    spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    fixture.nativeElement.querySelector('button[type="submit"]').click();

    expect(authService.login).toHaveBeenCalledWith({
      email: 'alice@example.com',
      password: 'password123'
    });
    expect(authService.saveToken).toHaveBeenCalledWith('test-token');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/products');
    expect(component.isSubmitting).toBeFalse();
  });

  // Shows the server error and resets the submitting state after failure.
  it('shows an API error when login fails', () => {
    component.form.setValue({ email: 'alice@example.com', password: 'password123' });
    authService.login.and.returnValue(throwError(() => new HttpErrorResponse({
      error: { message: 'Account is locked' },
      status: 401
    })));

    component.submit();
    fixture.detectChanges();

    expect(component.errorMessage).toBe('Account is locked');
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent)
      .toContain('Account is locked');
    expect(component.isSubmitting).toBeFalse();
    expect(authService.saveToken).not.toHaveBeenCalled();
  });
});