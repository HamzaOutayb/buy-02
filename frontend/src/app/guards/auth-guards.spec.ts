/// <reference types="jasmine" />

import { TestBed } from '@angular/core/testing';
import { firstValueFrom, Observable, of } from 'rxjs';
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
import { UserService } from '../services/user.service';

describe('route guards', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let userService: jasmine.SpyObj<UserService>;
  let router: Router;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['isAuthenticated', 'isSeller']
    );
    userService = jasmine.createSpyObj<UserService>('UserService', ['getCurrentUser']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: UserService, useValue: userService }
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

  it('redirects authenticated non-sellers to their profile', async () => {
    authService.isAuthenticated.and.returnValue(true);
    authService.isSeller.and.returnValue(false);
    userService.getCurrentUser.and.returnValue(of({
      id: 'user-1', name: 'Client', email: 'client@example.com', role: 'ROLE_CLIENT', avatar: null
    }));

    const result = await firstValueFrom(TestBed.runInInjectionContext(() =>
      sellerGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>);

    expect(router.serializeUrl(result as UrlTree)).toBe('/profile');
  });

  it('allows authenticated sellers to access seller routes', async () => {
    authService.isAuthenticated.and.returnValue(true);
    authService.isSeller.and.returnValue(true);
    userService.getCurrentUser.and.returnValue(of({
      id: 'seller-1', name: 'Seller', email: 'seller@example.com', role: 'ROLE_SELLER', avatar: null
    }));

    const result = await firstValueFrom(TestBed.runInInjectionContext(() =>
      sellerGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    ) as Observable<boolean | UrlTree>);

    expect(result).toBeTrue();
  });
});
