package com.mediaservice.mediaservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mediaservice.mediaservice.exception.MediaDeletionException;
import com.mediaservice.mediaservice.exception.MediaUploadException;
import com.mediaservice.mediaservice.models.Media;
import com.mediaservice.mediaservice.repository.MediaRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MediaService {

    private final Cloudinary cloudinary;
    private final MediaRepository mediaRepository;

    public MediaService(
            Cloudinary cloudinary,
            MediaRepository mediaRepository) {
        this.cloudinary = cloudinary;
        this.mediaRepository = mediaRepository;
    }

    public List<String> uploadImages(List<MultipartFile> files) {

        List<String> imagePaths = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                Map<?, ?> result = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "resource_type", "image"));
   
                String imagePath = result.get("secure_url").toString();

                if(imagePath == null || imagePath.isEmpty()) {
                    throw new MediaUploadException(
                            "Failed to upload image: secure_url is null or empty", null);
                }
                Media media = new Media();
                media.setImagePath(imagePath);
                mediaRepository.save(media);

                imagePaths.add(imagePath);

            } catch (IOException e) {
                throw new MediaUploadException(
                        "Failed to upload image",
                        e);
            }
        }

        return imagePaths;
    }

    public void deleteImages(List<String> images) {
        for (String imagePath : images) {
            try {
                String publicId = extractPublicId(imagePath);

                Map<?, ?> result = cloudinary.uploader().destroy(
                        publicId,
                        ObjectUtils.asMap(
                                "resource_type", "image"));
                String status = result.get("result").toString();
                if (!"ok".equals(status) && !"not found".equals(status)) {
                    throw new MediaDeletionException(
                            "Failed to delete image from storage: " + status);
                }
                mediaRepository.deleteByImagePath(imagePath);
            } catch (IOException e) {
                throw new MediaDeletionException(
                        "Failed to delete image from storage");
            }
        }

    }

    @KafkaListener(topics = "images-deleted", groupId = "media-service")
    public void deleteProductImages(ProductDeletedEvent event) {
        deleteImages(event.imagePaths());
    }

    private String extractPublicId(String imageUrl) {

        String uploadMarker = "/upload/";
        int uploadIndex = imageUrl.indexOf(uploadMarker);

        String publicId = imageUrl.substring(
                uploadIndex + uploadMarker.length());

        // Remove version: v123456789/
        publicId = publicId.replaceFirst("^v\\d+/", "");

        // Remove extension
        int extensionIndex = publicId.lastIndexOf(".");
        if (extensionIndex != -1) {
            publicId = publicId.substring(0, extensionIndex);
        }

        return publicId;
    }

    public record ProductDeletedEvent(
            List<String> imagePaths) {
    }
}
