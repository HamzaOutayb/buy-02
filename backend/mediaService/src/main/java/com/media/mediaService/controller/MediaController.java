package com.media.mediaService.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.media.mediaService.model.Media;
import com.media.mediaService.reposiroty.MediaRepository;
import com.media.mediaService.service.MediaService;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private static final int MAX_IMAGES_PER_UPLOAD = 5;

    private final MediaService mediaService;
    private final MediaRepository mediaRepository;

    public MediaController(MediaService mediaService, MediaRepository mediaRepository) {
        this.mediaService = mediaService;
        this.mediaRepository = mediaRepository;
    }

    @GetMapping("/products/{productId}")
    public List<String> getProductMedia(@PathVariable String productId) {
        return mediaRepository.findByProductId(productId).stream()
            .map(media -> media.getImagePath())
                .toList();
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        try {
            MediaService.StoredImage image = mediaService.fetchImage(media.getImagePath());
            return ResponseEntity.ok()
                    .contentType(image.contentType())
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                    .body(image.content());
        } catch (RestClientException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found", exception);
        }
    }

    @PostMapping("/images")
    @PreAuthorize("hasRole('SELLER')")
    public Map<String, String> uploadImage(@RequestPart("file") MultipartFile file,
            Authentication authentication) {
        try {
            String url = mediaService.uploadImage(file);
            return Map.of("id", url, "url", url);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload image", exception);
        }
    }

    @PostMapping("/upload/multiple")
    @PreAuthorize("hasRole('SELLER')")
    public Map<String, List<String>> uploadImages(@RequestPart("files") List<MultipartFile> files,
            Authentication authentication) {
        if (files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one image is required");
        }
        if (files.size() > MAX_IMAGES_PER_UPLOAD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can upload a maximum of 5 images per product");
        }

        List<String> urls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                urls.add(mediaService.uploadImage(file));
            }
            return Map.of("urls", urls);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload images", exception);
        }
    }

}