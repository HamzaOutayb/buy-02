package com.example.productservice.exception;

public class InvalidProductSearchException extends RuntimeException {
    public InvalidProductSearchException(String message) {
        super(message);
    }
}
