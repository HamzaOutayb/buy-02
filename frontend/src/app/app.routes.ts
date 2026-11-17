import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Home } from './pages/home/home';
import { Dashboard } from './pages/dashboard/dashboard';
import { ProductDetail } from './pages/product-detail/product-detail';
import { Profile } from './pages/profile/profile';
import { NotFound } from './pages/not-found/not-found';
import { Cart } from './pages/cart/cart';
import { Checkout } from './pages/checkout/checkout';
import { Orders } from './pages/orders/orders';
import { OrderDetail } from './pages/order-detail/order-detail';
import { SellerOrders } from './pages/seller-orders/seller-orders';
import { sellerGuard } from './guards/seller.guard';
import { authGuard } from './guards/auth.guard';
import { clientGuard } from './guards/client.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: Login,
  },
  {
    path: 'register',
    component: Register,
  },
  {
    path: 'profile',
    component: Profile,
    canActivate: [authGuard],
  },
  {
    path: 'cart',
    component: Cart,
    canActivate: [clientGuard],
  },
  {
    path: 'checkout',
    component: Checkout,
    canActivate: [clientGuard],
  },
  {
    path: 'orders/:id',
    component: OrderDetail,
    canActivate: [clientGuard],
  },
  {
    path: 'orders',
    component: Orders,
    canActivate: [clientGuard],
  },
  {
    path: 'products/:id',
    component: ProductDetail,
  },
  {
    path: 'dashboard/orders',
    component: SellerOrders,
    canActivate: [sellerGuard],
  },
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [sellerGuard],
  },
  {
    path: '',
    component: Home,
  },
  {
    path: '**',
    component: NotFound,
  },
];
