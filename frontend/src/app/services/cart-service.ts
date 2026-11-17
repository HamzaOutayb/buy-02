import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { EMPTY, Observable, Subject, throwError } from 'rxjs';
import { catchError, concatMap, finalize, map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface CartItem {
  productId: string;
  productName: string;
  price: number;
  quantity: number;
  imageUrl: string;
  sellerId: string;
  maxQuantity: number;
}

interface CartWrite {
  token: string;
  items: CartItem[];
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/cart`;
  private readonly itemsSignal = signal<CartItem[]>([]);
  private readonly writes = new Subject<CartWrite>();
  private pendingWrites = 0;

  readonly items = this.itemsSignal.asReadonly();
  readonly syncError = signal<string | null>(null);
  readonly isSyncing = signal(false);
  readonly count = computed(() => this.itemsSignal().reduce((acc, i) => acc + i.quantity, 0));
  readonly subtotal = computed(() =>
    this.itemsSignal().reduce((acc, i) => acc + i.price * i.quantity, 0),
  );

  constructor() {
    this.writes
      .pipe(
        concatMap(({ token, items }) => {
          if (localStorage.getItem('auth_token') !== token) {
            this.completeWrite();
            return EMPTY;
          }
          return this.http.put<CartItem[]>(this.apiUrl, {
            items: items.map(({ productId, quantity }) => ({ productId, quantity })),
          }).pipe(
            tap(() => this.syncError.set(null)),
            catchError((error: unknown) => {
              this.syncError.set(this.errorMessage(error));
              return EMPTY;
            }),
            finalize(() => this.completeWrite()),
          );
        }),
      )
      .subscribe();
  }

  loadForUser(): Observable<void> {
    const token = localStorage.getItem('auth_token');
    this.itemsSignal.set([]);
    if (!token) {
      this.resetForLogout();
      return throwError(() => new Error('You must be signed in to load a cart.'));
    }

    return this.http.get<CartItem[]>(this.apiUrl).pipe(
      tap((items) => {
        if (localStorage.getItem('auth_token') === token) {
          this.itemsSignal.set(items);
        }
        this.syncError.set(null);
      }),
      map(() => void 0),
      catchError((error: unknown) => {
        this.syncError.set(this.errorMessage(error));
        return throwError(() => error);
      }),
    );
  }

  addToCart(item: CartItem): void {
    if (!localStorage.getItem('auth_token')) return;
    const existing = this.itemsSignal().find((i) => i.productId === item.productId);
    if (existing) {
      this.itemsSignal.update((items) =>
        items.map((i) =>
          i.productId === item.productId
            ? { ...i, quantity: Math.min(i.quantity + item.quantity, i.maxQuantity) }
            : i,
        ),
      );
    } else {
      this.itemsSignal.update((items) => [...items, { ...item }]);
    }
    this.saveForCurrentUser();
  }

  removeFromCart(productId: string): void {
    if (!localStorage.getItem('auth_token')) return;
    this.itemsSignal.update((items) => items.filter((i) => i.productId !== productId));
    this.saveForCurrentUser();
  }

  updateQuantity(productId: string, quantity: number): void {
    if (!localStorage.getItem('auth_token')) return;
    this.itemsSignal.update((items) =>
      items.map((i) => {
        if (i.productId === productId) {
          const clamped = Math.max(1, Math.min(quantity, i.maxQuantity));
          return { ...i, quantity: clamped };
        }
        return i;
      }),
    );
    this.saveForCurrentUser();
  }

  clearCart(): void {
    if (!localStorage.getItem('auth_token')) {
      this.resetForLogout();
      return;
    }
    this.itemsSignal.set([]);
    this.saveForCurrentUser();
  }

  resetForLogout(): void {
    this.itemsSignal.set([]);
    this.syncError.set(null);
    localStorage.removeItem('cart_items');
  }

  getItems(): CartItem[] {
    return this.itemsSignal();
  }

  private saveForCurrentUser(): void {
    const token = localStorage.getItem('auth_token');
    if (!token) return;
    this.pendingWrites += 1;
    this.isSyncing.set(true);
    this.writes.next({ token, items: this.itemsSignal() });
  }

  private completeWrite(): void {
    if (!this.isSyncing()) return;
    this.pendingWrites -= 1;
    this.isSyncing.set(this.pendingWrites > 0);
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      return error.error?.message ?? 'Could not sync your cart. Please try again.';
    }
    return 'Could not sync your cart. Please try again.';
  }
}
