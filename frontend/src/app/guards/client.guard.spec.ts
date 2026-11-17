import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { clientGuard } from './client.guard';

describe('clientGuard', () => {
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('redirects signed-out users to login', () => {
    const result = TestBed.runInInjectionContext(() => clientGuard({} as never, {} as never));

    expect((result as import('@angular/router').UrlTree).toString())
      .toBe(router.parseUrl('/login').toString());
    httpTesting.expectNone('https://localhost:8080/api/v1/users/me');
  });

  it('loads the user profile and rejects sellers from client-only routes', async () => {
    localStorage.setItem('auth_token', 'seller-token');
    const result = TestBed.runInInjectionContext(() => clientGuard({} as never, {} as never));
    const decision = new Promise((resolve) => {
      (result as import('rxjs').Observable<unknown>).subscribe(resolve);
    });

    httpTesting.expectOne('https://localhost:8080/api/v1/users/me').flush({
      id: 'seller-1',
      name: 'Seller',
      email: 'seller@example.com',
      avatar: null,
      role: 'SELLER',
    });

    expect((await decision as import('@angular/router').UrlTree).toString())
      .toBe(router.parseUrl('/').toString());
    httpTesting.expectNone('https://localhost:8080/api/v1/cart');
  });
});
