import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { API_BASE_URL } from '../helpers/api-url';
import { UserProfile } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly currentUserState = signal<UserProfile | null>(null);

  readonly currentUser = this.currentUserState.asReadonly();

  getCurrentUser(): Observable<UserProfile>
  {
    return this.http.get<UserProfile>(`${API_BASE_URL}/me`).pipe(
      tap((user) => this.currentUserState.set(user))
    );
  }

  updateAvatar(avatar: string): Observable<UserProfile>
  {
    return this.http.put<UserProfile>(`${API_BASE_URL}/me/avatar`, { avatar }).pipe(
      tap((user) => this.currentUserState.set(user))
    );
  }

  clearCurrentUser(): void
  {
    this.currentUserState.set(null);
  }
}
