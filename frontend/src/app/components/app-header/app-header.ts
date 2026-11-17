import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../services/cart-service';
import { UserContextService } from '../../services/user-context-service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="border-b border-slate-200 bg-[#f4f1eb]">
      <nav class="mx-auto flex max-w-7xl items-center justify-between gap-4 px-5 py-5 sm:px-8" aria-label="Main navigation">
        <a routerLink="/" class="shrink-0 text-xl font-black tracking-tight">buy<span class="text-teal-700">/01</span></a>
        <div class="flex flex-wrap items-center justify-end gap-3 text-sm font-bold">
          <a routerLink="/" class="text-slate-600 transition hover:text-teal-700">Browse</a>
          @if (userLoading()) {
            <span class="h-8 w-24 animate-pulse rounded-full bg-slate-200" aria-label="Loading account"></span>
          } @else if (user(); as currentUser) {
            @if (currentUser.role === 'CLIENT') {
              <a routerLink="/cart" class="relative text-slate-700 transition hover:text-teal-700" aria-label="Shopping cart">
                <span aria-hidden="true">🛒</span><span class="hidden sm:inline"> Cart</span>
                @if (cartCount() > 0) {
                  <span class="ml-1 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-teal-800 px-1.5 text-[11px] text-white">{{ cartCount() }}</span>
                }
              </a>
              <a routerLink="/orders" class="text-slate-600 transition hover:text-teal-700">Orders</a>
            } @else {
              <a routerLink="/dashboard" class="text-slate-600 transition hover:text-teal-700">Products</a>
              <a routerLink="/dashboard/orders" class="text-slate-600 transition hover:text-teal-700">Orders</a>
            }
            <a routerLink="/profile" class="hidden text-slate-600 transition hover:text-teal-700 sm:inline">Hi, {{ currentUser.name }}</a>
            <button type="button" (click)="logout()" class="rounded-full border border-slate-300 px-4 py-2 text-slate-700 transition hover:border-teal-700 hover:text-teal-700">Sign out</button>
          } @else {
            <a routerLink="/login" class="text-slate-600 transition hover:text-teal-700">Sign in</a>
            <a routerLink="/register" class="rounded-full bg-[#172a2b] px-4 py-2 text-white transition hover:bg-teal-800">Create account</a>
          }
        </div>
      </nav>
    </header>
  `,
})
export class AppHeader {
  private readonly userContext = inject(UserContextService);
  private readonly router = inject(Router);
  readonly user = this.userContext.user;
  readonly userLoading = this.userContext.isLoading;
  readonly cartCount = inject(CartService).count;

  logout(): void {
    this.userContext.logout();
    this.router.navigateByUrl('/');
  }
}
