import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { CartService } from './cart-service';

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  avatar: string | null;
  role: 'CLIENT' | 'SELLER';
}

@Injectable({ providedIn: 'root' })
export class UserContextService {
  private readonly http = inject(HttpClient);
  private readonly cartService = inject(CartService);
  private readonly apiUrl = `${environment.apiBaseUrl}/users`;
  private readonly userSignal = signal<UserProfile | null>(null);
  private loadRequest?: { token: string; request: Observable<void> };

  readonly user = this.userSignal.asReadonly();
  readonly isLoading = signal(true);

  load(): Observable<void> {
    const token = localStorage.getItem('auth_token');
    if (!token) {
      this.userSignal.set(null);
      this.cartService.resetForLogout();
      this.isLoading.set(false);
      return of(void 0);
    }
    if (this.loadRequest?.token === token) return this.loadRequest.request;

    this.isLoading.set(true);
    this.userSignal.set(null);
    this.cartService.resetForLogout();
    let request: Observable<void>;
    request = this.http.get<UserProfile>(`${this.apiUrl}/me`).pipe(
      tap((user) => {
        if (localStorage.getItem('auth_token') === token) this.userSignal.set(user);
      }),
      catchError(() => {
        if (localStorage.getItem('auth_token') === token) {
          localStorage.removeItem('auth_token');
          this.userSignal.set(null);
          this.cartService.resetForLogout();
        }
        return of(null);
      }),
      switchMap((user) => {
        if (user?.role === 'CLIENT' && localStorage.getItem('auth_token') === token) {
          return this.cartService.loadForUser().pipe(catchError(() => of(void 0)));
        }
        if (localStorage.getItem('auth_token') === token) this.cartService.resetForLogout();
        return of(void 0);
      }),
      finalize(() => {
        if (this.loadRequest?.request === request) {
          this.isLoading.set(false);
          this.loadRequest = undefined;
        }
      }),
      map(() => void 0),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.loadRequest = { token, request };
    return request;
  }

  updateProfile(
    profile: Omit<UserProfile, 'id' | 'role'> & { password: string },
  ): Observable<UserProfile> {
    return this.http
      .put<UserProfile>(`${this.apiUrl}/me`, profile)
      .pipe(tap((user) => this.userSignal.set(user)));
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    this.userSignal.set(null);
    this.cartService.resetForLogout();
    this.isLoading.set(false);
  }
}
