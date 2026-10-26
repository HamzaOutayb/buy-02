package com.mediaservice.mediaservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.mediaservice.mediaservice.exception.MediaDeletionException;
import com.mediaservice.mediaservice.exception.MediaUploadException;
import com.mediaservice.mediaservice.models.Media;
import com.mediaservice.mediaservice.repository.MediaRepository;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private MediaRepository mediaRepository;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(cloudinary, mediaRepository);
    }

    private void stubCloudinaryUploader() {
        when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void uploadImages_uploadsEachFileAndStoresItsUrl() throws Exception {
        stubCloudinaryUploader();
        MultipartFile first = new MockMultipartFile("images", "one.jpg", "image/jpeg", new byte[] { 1, 2 });
        MultipartFile second = new MockMultipartFile("images", "two.png", "image/png", new byte[] { 3, 4 });
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "https://cdn.example/one.jpg"))
                .thenReturn(Map.of("secure_url", "https://cdn.example/two.png"));

        List<String> paths = mediaService.uploadImages(List.of(first, second));

        assertEquals(List.of("https://cdn.example/one.jpg", "https://cdn.example/two.png"), paths);
        ArgumentCaptor<Media> savedMedia = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository, org.mockito.Mockito.times(2)).save(savedMedia.capture());
        assertEquals("https://cdn.example/two.png", savedMedia.getAllValues().get(1).getImagePath());
    }

    @Test
    void uploadImagesReturnsEmptyListWhenThereAreNoFiles() {
        assertEquals(List.of(), mediaService.uploadImages(List.of()));

        verifyNoInteractions(uploader, mediaRepository);
    }

    @Test
    void uploadImages_wrapsCloudinaryIoErrors() throws Exception {
        stubCloudinaryUploader();
        MultipartFile file = new MockMultipartFile("images", "broken.jpg", "image/jpeg", new byte[] { 1 });
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Cloudinary unavailable"));

        assertThrows(MediaUploadException.class, () -> mediaService.uploadImages(List.of(file)));
        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    void uploadImagesRejectsAnEmptySecureUrl() throws Exception {
        stubCloudinaryUploader();
        MultipartFile file = new MockMultipartFile("images", "empty.jpg", "image/jpeg", new byte[] { 1 });
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", ""));

        assertThrows(MediaUploadException.class, () -> mediaService.uploadImages(List.of(file)));

        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    void deleteImages_extractsPublicIdDeletesFromCloudinaryAndRemovesRecord() throws Exception {
        stubCloudinaryUploader();
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v123/folder/photo.jpg";
        when(uploader.destroy(eq("folder/photo"), anyMap())).thenReturn(Map.of("result", "ok"));

        mediaService.deleteImages(List.of(imageUrl));

        verify(uploader).destroy(eq("folder/photo"), anyMap());
        verify(mediaRepository).deleteByImagePath(imageUrl);
    }

    @Test
    void deleteImages_acceptsAlreadyMissingCloudinaryImages() throws Exception {
        stubCloudinaryUploader();
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/photo.jpg";
        when(uploader.destroy(eq("photo"), anyMap())).thenReturn(Map.of("result", "not found"));

        mediaService.deleteImages(List.of(imageUrl));

        verify(mediaRepository).deleteByImagePath(imageUrl);
    }

    @Test
    void deleteImagesDoesNothingForAnEmptyList() {
        mediaService.deleteImages(List.of());

        verifyNoInteractions(uploader, mediaRepository);
    }

    @Test
    void deleteImages_throwsWhenCloudinaryRejectsDeletion() throws Exception {
        stubCloudinaryUploader();
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/photo.jpg";
        when(uploader.destroy(eq("photo"), anyMap())).thenReturn(Map.of("result", "error"));

        assertThrows(MediaDeletionException.class, () -> mediaService.deleteImages(List.of(imageUrl)));
        verify(mediaRepository, never()).deleteByImagePath(imageUrl);
    }

    @Test
    void deleteImagesWrapsCloudinaryIoErrorsAndKeepsDatabaseRecord() throws Exception {
        stubCloudinaryUploader();
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/photo.jpg";
        when(uploader.destroy(eq("photo"), anyMap())).thenThrow(new IOException("Cloudinary unavailable"));

        assertThrows(MediaDeletionException.class, () -> mediaService.deleteImages(List.of(imageUrl)));

        verify(mediaRepository, never()).deleteByImagePath(imageUrl);
    }

    @Test
    void deleteProductImagesDeletesImagesFromTheKafkaEvent() throws Exception {
        stubCloudinaryUploader();
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v123/photo.jpg";
        when(uploader.destroy(eq("photo"), anyMap())).thenReturn(Map.of("result", "ok"));

        mediaService.deleteProductImages(new MediaService.ProductDeletedEvent(List.of(imageUrl)));

        verify(uploader).destroy(eq("photo"), anyMap());
        verify(mediaRepository).deleteByImagePath(imageUrl);
    }
}
