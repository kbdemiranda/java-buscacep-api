package io.github.kbdemiranda.buscacep.controller;

import io.github.kbdemiranda.buscacep.dto.ErrorResponseDTO;
import io.github.kbdemiranda.buscacep.exception.CepNotFoundException;
import io.github.kbdemiranda.buscacep.exception.ExternalCepClientException;
import io.github.kbdemiranda.buscacep.exception.InvalidCepException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CepNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(CepNotFoundException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "CEP não encontrado", request);
    }

    @ExceptionHandler(InvalidCepException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidCep(InvalidCepException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(ExternalCepClientException.class)
    public ResponseEntity<ErrorResponseDTO> handleExternalClientError(
        ExternalCepClientException e,
        HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_GATEWAY, "Falha ao consultar provedor externo", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(
        MethodArgumentNotValidException e,
        HttpServletRequest request
    ) {
        String message = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getDefaultMessage())
            .filter(Objects::nonNull)
            .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, fallbackMessage(message), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(
        ConstraintViolationException e,
        HttpServletRequest request
    ) {
        String message = e.getConstraintViolations().stream()
            .map(violation -> violation.getMessage())
            .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, fallbackMessage(message), request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleHandlerMethodValidation(
        HandlerMethodValidationException e,
        HttpServletRequest request
    ) {
        String message = e.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream())
            .map(error -> error.getDefaultMessage() == null ? "Validation error" : error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, fallbackMessage(message), request);
    }

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        BindException.class,
        HttpMessageConversionException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleConversionErrors(Exception e, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Parâmetro inválido", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, fallbackMessage(e.getMessage()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(Exception e, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponseDTO body = new ErrorResponseDTO(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            fallbackMessage(message),
            request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    private String fallbackMessage(String message) {
        return (message == null || message.isBlank()) ? "Erro de validação" : message;
    }
}
