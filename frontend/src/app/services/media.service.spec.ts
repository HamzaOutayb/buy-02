/// <reference types="jasmine" />

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../helpers/api-url';
import { MediaService } from './media.service';

describe('MediaService', () => {
  let mediaService: MediaService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    mediaService = TestBed.inject(MediaService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTestingController.verify());

  it('should upload all files as images in multipart form data', () => {
    const first = new File(['one'], 'one.jpg', { type: 'image/jpeg' });
    const second = new File(['two'], 'two.png', { type: 'image/png' });
    let response: string[] | undefined;

    mediaService.uploadImages([first, second]).subscribe((urls) => response = urls);

    const request = httpTestingController.expectOne(`${API_BASE_URL}/media/images`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body instanceof FormData).toBeTrue();
    expect(request.request.body.getAll('images')).toEqual([first, second]);

    request.flush(['https://cdn.example/one.jpg', 'https://cdn.example/two.png']);
    expect(response).toEqual(['https://cdn.example/one.jpg', 'https://cdn.example/two.png']);
  });

  it('should send every image URL as a repeated query parameter when deleting', () => {
    const urls = ['https://cdn.example/one.jpg', 'https://cdn.example/two.png'];
    let completed = false;

    mediaService.deleteImages(urls).subscribe(() => completed = true);

    const request = httpTestingController.expectOne((req) =>
      req.url === `${API_BASE_URL}/media/images`
    );
    expect(request.request.method).toBe('DELETE');
    expect(request.request.params.getAll('images')).toEqual(urls);

    request.flush(null);
    expect(completed).toBeTrue();
  });
});
