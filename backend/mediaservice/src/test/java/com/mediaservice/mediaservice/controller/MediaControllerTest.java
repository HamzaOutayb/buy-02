package com.mediaservice.mediaservice.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mediaservice.mediaservice.exception.BadRequestException;
import com.mediaservice.mediaservice.exception.GlobalExceptionHandler;
import com.mediaservice.mediaservice.exception.MediaUploadException;
import com.mediaservice.mediaservice.service.MediaService;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mediaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadImagesReturnsUploadedUrlsForValidImages() throws Exception {
        when(mediaService.uploadImages(anyList()))
                .thenReturn(List.of("https://cdn.example/one.jpg", "https://cdn.example/two.png"));

        mockMvc.perform(multipart("/media/images")
                        .file(image("one.jpg", "image/jpeg", new byte[] { 1 }))
                        .file(image("two.png", "image/png", new byte[] { 2 })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("https://cdn.example/one.jpg"))
                .andExpect(jsonPath("$[1]").value("https://cdn.example/two.png"));

        verify(mediaService).uploadImages(anyList());
    }

    @Test
    void uploadImagesRejectsAnEmptyImageList() {
        assertThrows(BadRequestException.class, () -> mediaController.uploadImages(List.of()));

        verify(mediaService, never()).uploadImages(anyList());
    }

    @Test
    void uploadImagesRejectsMoreThanFiveFilesWithoutCallingService() throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/media/images");
        for (int index = 0; index < 6; index++) {
            request.file(image("image-" + index + ".jpg", "image/jpeg", new byte[] { 1 }));
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A maximum of five images can be uploaded at once"))
                .andExpect(jsonPath("$.path").value("/media/images"));

        verify(mediaService, never()).uploadImages(anyList());
    }

    @Test
    void uploadImagesRejectsNonImageFilesWithoutCallingService() throws Exception {
        mockMvc.perform(multipart("/media/images")
                        .file(image("notes.pdf", "application/pdf", new byte[] { 1 })))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only image files can be uploaded"));

        verify(mediaService, never()).uploadImages(anyList());
    }

    @Test
    void uploadImagesRejectsFilesLargerThanTwoMegabytesWithoutCallingService() throws Exception {
        byte[] oversizedImage = new byte[2 * 1024 * 1024 + 1];

        mockMvc.perform(multipart("/media/images")
                        .file(image("large.jpg", "image/jpeg", oversizedImage)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Each image must be 2 MB or smaller"));

        verify(mediaService, never()).uploadImages(anyList());
    }

    @Test
    void uploadImagesRejectsCombinedImagesLargerThanSevenMegabytesWithoutCallingService() throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/media/images");
        byte[] imageContent = new byte[1_500 * 1024];
        for (int index = 0; index < 5; index++) {
            request.file(image("image-" + index + ".jpg", "image/jpeg", imageContent));
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Combined image size must be 7 MB or smaller per upload"));

        verify(mediaService, never()).uploadImages(anyList());
    }

    @Test
    void uploadImagesMapsStorageFailureToBadGateway() throws Exception {
        when(mediaService.uploadImages(anyList()))
                .thenThrow(new MediaUploadException("Failed to upload image", new IOException("storage down")));

        mockMvc.perform(multipart("/media/images")
                        .file(image("one.jpg", "image/jpeg", new byte[] { 1 })))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Failed to upload image"));
    }

    @Test
    void deleteImagesDelegatesAllUrlsAndReturnsOk() throws Exception {
        String firstUrl = "https://cdn.example/one.jpg";
        String secondUrl = "https://cdn.example/two.jpg";

        mockMvc.perform(delete("/media/images").param("images", firstUrl, secondUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(mediaService).deleteImages(List.of(firstUrl, secondUrl));
    }

    private MockMultipartFile image(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("images", filename, contentType, content);
    }
}
