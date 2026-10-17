/// <reference types="jasmine" />

import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { UserRole } from '../../models/auth.model';
import { AuthService } from '../../services/auth.service';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Starts with required fields empty and the customer role selected.
  it('starts with required fields invalid and the default role selected', () => {
    expect(component.form.invalid).toBeTrue();
    expect(component.form.controls.role.value).toBe(UserRole.Client);
  });

  // Rejects whitespace-only names and malformed email addresses.
  it('validates name and email formats', () => {
    component.form.controls.name.setValue('\t  \n');
    component.form.controls.password.setValue('short');

    expect(component.form.controls.name.hasError('pattern')).toBeTrue();
    expect(component.form.controls.password.hasError('minlength')).toBeTrue();

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
  });

  // Enforces the password length and text field limits in the registration form.
  it('rejects values outside the form length limits', () => {
    component.form.controls.name.setValue('n'.repeat(101));
    component.form.controls.email.setValue(`a${'b'.repeat(245)}@example.com`);
    component.form.controls.password.setValue('p'.repeat(73));

    expect(component.form.controls.name.hasError('maxlength')).toBeTrue();
    expect(component.form.controls.email.hasError('maxlength')).toBeTrue();
    expect(component.form.controls.password.hasError('maxlength')).toBeTrue();
  });

  it('accepts values at the name, email, and password length boundaries', () => {
    const emailAtLimit = `${'a'.repeat(64)}@${[
      'b'.repeat(63),
      'c'.repeat(63),
      'd'.repeat(61)
    ].join('.')}`;
    component.form.setValue({
      name: 'n'.repeat(100),
      email: emailAtLimit,
      password: 'p'.repeat(8),
      role: UserRole.Client
    });

    expect(emailAtLimit.length).toBe(254);
    expect(component.form.valid).toBeTrue();

    component.form.controls.password.setValue('p'.repeat(72));

    expect(component.form.controls.password.valid).toBeTrue();
    expect(component.form.valid).toBeTrue();
  });

  // Submits the rendered form, selecting the requested role and normalizing values
  it('registers with normalized name and email', () => {
    const nameInput: HTMLInputElement = fixture.nativeElement.querySelector('#name');
    const emailInput: HTMLInputElement = fixture.nativeElement.querySelector('#email');
    const passwordInput: HTMLInputElement = fixture.nativeElement.querySelector('#password');
    nameInput.value = ' Alice Smith ';
    nameInput.dispatchEvent(new Event('input'));
    emailInput.value = ' alice@example.com ';
    emailInput.dispatchEvent(new Event('input'));
    passwordInput.value = 'password123';
    passwordInput.dispatchEvent(new Event('input'));
    const roleInputs: NodeListOf<HTMLInputElement> = fixture.nativeElement.querySelectorAll(
      'input[type="radio"]'
    );
    roleInputs[1].click();
    authService.register.and.returnValue(of({
      id: 'user-1',
      name: 'Alice Smith',
      email: 'alice@example.com',
      role: 'SELLER'
    }));

    fixture.nativeElement.querySelector('button[type="submit"]').click();
    fixture.detectChanges();

    expect(authService.register).toHaveBeenCalledWith({
      name: 'Alice Smith',
      email: 'alice@example.com',
      password: 'password123',
      role: UserRole.Seller
    });
    expect(component.successMessage).toBe('Account created. You can now sign in.');
    expect(fixture.nativeElement.querySelector('[role="status"]')?.textContent)
      .toContain('Account created. You can now sign in.');
    expect(fixture.nativeElement.querySelector('a[routerLink="/login"]')).not.toBeNull();
    expect(component.isSubmitting).toBeFalse();
  });

  // Does not call the registration service when any required field is invalid.
  it('does not submit an invalid registration form', () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(component.form.controls.name.touched).toBeTrue();
    expect(component.form.controls.email.touched).toBeTrue();
    expect(component.form.controls.password.touched).toBeTrue();
    expect(fixture.nativeElement.querySelectorAll('[role="alert"]').length).toBe(3);
    expect(authService.register).not.toHaveBeenCalled();
  });

  // Shows an API error and resets the submitting state if registration fails.
  it('shows an API error when registration fails', () => {
    component.form.setValue({
      name: 'Alice',
      email: 'alice@example.com',
      password: 'password123',
      role: UserRole.Client
    });
    authService.register.and.returnValue(throwError(() => new HttpErrorResponse({
      error: { message: 'Email is already in use' },
      status: 409
    })));

    component.submit();
    fixture.detectChanges();

    expect(component.errorMessage).toBe('Email is already in use');
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent)
      .toContain('Email is already in use');
    expect(component.isSubmitting).toBeFalse();
  });
});