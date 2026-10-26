import { Routes } from '@angular/router';
import { authGuard } from '../guards/auth.guard';
import { LoginComponent } from '../components/login/login.component';
import { ProfileComponent } from '../components/profile/profile.component';
import { ProductsComponent } from '../components/products/products.component';
import { RegisterComponent } from '../components/register/register.component';
import { ProductFormComponent } from '../components/product-form/product-form.component';
import { SellerProductsComponent } from '../components/seller-products/seller-products.component';
import { ProductDetailsComponent } from '../components/product-details/product-details.component';
import { sellerGuard } from '../guards/seller.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'products'
  },
  {
    path: 'products/:id',
    component: ProductDetailsComponent
  },
  {
    path: 'products',
    component: ProductsComponent
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [authGuard]
  },
  {
    path: 'seller/products',
    component: SellerProductsComponent,
    canActivate: [sellerGuard]
  },
  {
    path: 'seller/products/new',
    component: ProductFormComponent,
    canActivate: [sellerGuard]
  },
  {
    path: 'seller/products/:id/edit',
    component: ProductFormComponent,
    canActivate: [sellerGuard]
  },
  {
    path: '**',
    redirectTo: 'products'
  }
];
