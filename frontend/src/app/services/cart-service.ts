import { Injectable, computed, signal } from '@angular/core';

export interface CartItem {
  productId: string;
  productName: string;
  price: number;
  quantity: number;
  imageUrl: string;
  sellerId: string;
  maxQuantity: number;
}

const STORAGE_KEY = 'cart_items';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly itemsSignal = signal<CartItem[]>([]);

  readonly items = this.itemsSignal.asReadonly();
  readonly count = computed(() => this.itemsSignal().reduce((acc, i) => acc + i.quantity, 0));
  readonly subtotal = computed(() =>
    this.itemsSignal().reduce((acc, i) => acc + i.price * i.quantity, 0),
  );

  constructor() {
    this.loadFromStorage();
  }

  addToCart(item: CartItem): void {
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
    this.saveToStorage();
  }

  removeFromCart(productId: string): void {
    this.itemsSignal.update((items) => items.filter((i) => i.productId !== productId));
    this.saveToStorage();
  }

  updateQuantity(productId: string, quantity: number): void {
    this.itemsSignal.update((items) =>
      items.map((i) => {
        if (i.productId === productId) {
          const clamped = Math.max(1, Math.min(quantity, i.maxQuantity));
          return { ...i, quantity: clamped };
        }
        return i;
      }),
    );
    this.saveToStorage();
  }

  clearCart(): void {
    this.itemsSignal.set([]);
    this.saveToStorage();
  }

  getItems(): CartItem[] {
    return this.itemsSignal();
  }

  private saveToStorage(): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.itemsSignal()));
    } catch {
      // ignore storage errors
    }
  }

  private loadFromStorage(): void {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as CartItem[];
        if (Array.isArray(parsed)) {
          this.itemsSignal.set(parsed);
        }
      }
    } catch {
      // ignore parse errors
    }
  }
}
