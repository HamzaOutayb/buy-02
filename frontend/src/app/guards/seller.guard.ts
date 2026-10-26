import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { UserService } from '../services/user.service';

export const sellerGuard: CanActivateFn = () =>
{
  const authService = inject(AuthService);
  const userService = inject(UserService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  return userService.getCurrentUser().pipe(
    map(() => authService.isSeller()
      ? true
      : router.createUrlTree(['/profile'])),
    catchError(() => [router.createUrlTree(['/profile'])])
  );
};
