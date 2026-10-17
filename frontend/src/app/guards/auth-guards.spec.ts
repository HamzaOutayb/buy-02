/// <reference types="jasmine" />

import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree
} from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';
import { sellerGuard } from './seller.guard';

describe('route guards', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['isAuthenticated', 'isSeller']
    );

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService }
      ]
    });
    router = TestBed.inject(Router);
  });

  it('allows authenticated users to access protected routes', () => {
    authService.isAuthenticated.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    expect(result).toBeTrue();
  });

  it('redirects unauthenticated users to sign in', () => {
    authService.isAuthenticated.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });

  it('redirects unauthenticated users away from seller routes to sign in', () => {
    authService.isAuthenticated.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      sellerGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
    expect(authService.isSeller).not.toHaveBeenCalled();
  });

  it('redirects authenticated non-sellers to their profile', () => {
    authService.isAuthenticated.and.returnValue(true);
    authService.isSeller.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      sellerGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    expect(router.serializeUrl(result as UrlTree)).toBe('/profile');
  });

  it('allows authenticated sellers to access seller routes', () => {
    authService.isAuthenticated.and.returnValue(true);
    authService.isSeller.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      sellerGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    expect(result).toBeTrue();
  });
});