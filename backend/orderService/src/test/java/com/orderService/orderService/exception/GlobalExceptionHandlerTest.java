package com.orderService.orderService.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsStatusAndMissingRouteExceptions() {
        var statusResponse = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "Already exists"));
        var notFoundResponse = handler.handleNotFound(mock(NoResourceFoundException.class));

        assertEquals(Map.of("status", 409, "message", "Already exists"), statusResponse.getBody());
        assertEquals(Map.of("status", 404, "message", "Route not found"), notFoundResponse.getBody());
    }

    @Test
    void returnsFirstFieldValidationError() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("request", "quantity", "must be at least 1")));

        var response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("status", 400, "message", "quantity must be at least 1"), response.getBody());
    }

    @Test
    void returnsFallbackForEmptyValidationErrorsAndMalformedBody() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        var emptyValidationResponse = handler.handleValidation(exception);
        var malformedBodyResponse = handler.handleValidation(mock(HttpMessageNotReadableException.class));

        assertEquals(Map.of("status", 400, "message", "Request validation failed"), emptyValidationResponse.getBody());
        assertEquals(Map.of("status", 400, "message", "Malformed request body"), malformedBodyResponse.getBody());
    }

    @Test
    void mapsUnexpectedExceptions() {
        var response = handler.handleUnexpected(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Map.of("status", 500, "message", "An unexpected server error occurred"), response.getBody());
    }
}
