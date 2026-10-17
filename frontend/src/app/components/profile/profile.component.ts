import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { getApiErrorMessage } from '../../helpers/api-error-message';
import { UserProfile } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly mediaService = inject(MediaService);
  private readonly userService = inject(UserService);

  readonly isSeller = this.authService.isSeller();

  currentUser: UserProfile | null = null;
  isLoading = true;
  isUploadingAvatar = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void
  {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'Your account information could not be loaded.'
        );
        this.isLoading = false;
      }
    });
  }

  onAvatarSelected(event: Event): void
  {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';

    if (!file) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.isUploadingAvatar = true;

    this.mediaService.uploadImages([file]).subscribe({
      next: (urls) => {
        const avatarUrl = urls[0];

        if (!avatarUrl) {
          this.errorMessage = 'The image was uploaded but no URL was returned.';
          this.isUploadingAvatar = false;
          return;
        }

        this.userService.updateAvatar(avatarUrl).subscribe({
          next: (user) => {
            this.currentUser = user;
            this.successMessage = 'Profile photo updated.';
            this.isUploadingAvatar = false;
          },
          error: (error: HttpErrorResponse) => {
            this.errorMessage = getApiErrorMessage(
              error,
              'The profile photo URL could not be saved.'
            );
            this.isUploadingAvatar = false;
          }
        });
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = getApiErrorMessage(
          error,
          'The image could not be uploaded. Choose an image up to 2 MB.'
        );
        this.isUploadingAvatar = false;
      }
    });
  }
}
