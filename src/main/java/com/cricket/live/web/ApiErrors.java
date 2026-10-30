package com.cricket.live.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> notFound(Exception e) { return err(HttpStatus.NOT_FOUND, e.getMessage()); }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(Exception e) { return err(HttpStatus.CONFLICT, e.getMessage()); }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<Map<String, String>> badRequest(Exception e) { return err(HttpStatus.BAD_REQUEST, "Invalid request: " + e.getMessage()); }

    private ResponseEntity<Map<String, String>> err(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("error", String.valueOf(msg)));
    }
}
