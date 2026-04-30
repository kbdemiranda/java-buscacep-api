package io.github.kbdemiranda.buscacep.controller;

import io.github.kbdemiranda.buscacep.dto.ErrorResponseDTO;
import io.github.kbdemiranda.buscacep.exception.CepNotFoundException;
import io.github.kbdemiranda.buscacep.exception.ExternalCepClientException;
import io.github.kbdemiranda.buscacep.exception.InvalidCepException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(CepNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(CepNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidCepException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidCep(InvalidCepException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ExternalCepClientException.class)
    public ResponseEntity<ErrorResponseDTO> handleExternalClientError(ExternalCepClientException e) {
        return buildResponse(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String message) {
        ErrorResponseDTO body = new ErrorResponseDTO(status.value(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
