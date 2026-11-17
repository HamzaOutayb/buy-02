import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UserContextService } from './services/user-context-service';
import { AppHeader } from './components/app-header/app-header';

@Component({
  selector: 'app-root',
  imports: [AppHeader, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly userContext = inject(UserContextService);
  protected readonly title = signal('frontend');

  constructor() {
    this.userContext.load().subscribe();
  }
}
