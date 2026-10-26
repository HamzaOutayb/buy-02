package com.mediaservice.mediaservice.controller;

import com.mediaservice.mediaservice.exception.BadRequestException;
import com.mediaservice.mediaservice.service.MediaService;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
public class MediaController {

    private static final int MAX_IMAGES = 5;
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024;

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/images")
    public ResponseEntity<List<String>> uploadImages(
            @RequestParam("images") List<MultipartFile> images) {

        validateImages(images);

        List<String> imageUrls = mediaService.uploadImages(images);
        return ResponseEntity.ok(imageUrls);
    }

    @DeleteMapping("/images")
    public ResponseEntity<List<String>> deleteImages(
            @RequestParam("images") List<String> images) {

        mediaService.deleteImages(images);
        return ResponseEntity.ok().build();
    }

    private void validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new BadRequestException(
                    "At least one image is required");
        }

        if (images.size() > MAX_IMAGES) {
            throw new BadRequestException(
                    "A maximum of five images can be uploaded at once");
        }

        for (MultipartFile image : images) {
            String contentType = image.getContentType();

            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BadRequestException(
                            "Only image files can be uploaded");
            }

            if (image.getSize() > MAX_IMAGE_SIZE) {
                throw new BadRequestException(
                        "Each image must be 2 MB or smaller");
            }
        }
    }
}
