import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../helpers/api-url';

@Injectable({
  providedIn: 'root'
})
export class MediaService {
  private readonly http = inject(HttpClient);

  uploadImages(files: File[]): Observable<string[]>
  {
    const formData = new FormData();

    for (const file of files) {
      formData.append('images', file);
    }

    return this.http.post<string[]>(`${API_BASE_URL}/media/images`, formData);
  }

  deleteImages(imageUrls: string[]): Observable<void>
  {
    let params = new HttpParams();

    for (const imageUrl of imageUrls) {
      params = params.append('images', imageUrl);
    }

    return this.http.delete<void>(`${API_BASE_URL}/media/images`, { params });
  }
}
