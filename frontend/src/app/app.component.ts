import { Component, effect, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { UserService } from './services/user.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);

  readonly authenticated = this.authService.authenticated;
  readonly currentUser = this.userService.currentUser;

  get seller(): boolean {
    return this.authenticated() && this.authService.isSeller();
  }

  constructor()
  {
    effect((onCleanup) => {
      if (!this.authenticated()) {
        this.userService.clearCurrentUser();
        return;
      }

      const subscription = this.userService.getCurrentUser().subscribe({
        error: () => this.userService.clearCurrentUser()
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  logout(): void
  {
    this.authService.logout();
    this.userService.clearCurrentUser();
    void this.router.navigateByUrl('/products');
  }
}
