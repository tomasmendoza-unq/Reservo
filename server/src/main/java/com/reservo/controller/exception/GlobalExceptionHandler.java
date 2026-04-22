package com.reservo.controller.exception;

import com.reservo.service.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja todas las excepciones de regla de negocio (BusinessException)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseErrorDTO> handleBusinessException(
        BusinessException ex, HttpServletRequest request) {
        log.warn("Business error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ResponseErrorDTO.of(
                ex.getHttpStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
            ));
    }

    /**
     * Maneja excepciones de validación de Bean Validation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseErrorDTO> handleValidationExceptions(
        MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldError() != null
            ? ex.getBindingResult().getFieldError().getDefaultMessage()
            : "Validation failed";
        log.warn("Validation error: {}", message);
        return ResponseEntity
            .badRequest()
            .body(ResponseErrorDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message,
                request.getRequestURI()
            ));
    }

    /**
     * Maneja excepciones de violación de constraints
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseErrorDTO> handleConstraintViolationException(
        ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ResponseErrorDTO.of(
                HttpStatus.BAD_REQUEST.value(),
                "CONSTRAINT_VIOLATION",
                ex.getMessage(),
                request.getRequestURI()
            ));
    }

    /**
     * Maneja todas las excepciones no capturadas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseErrorDTO> handleGenericException(
        Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ResponseErrorDTO.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request.getRequestURI()
            ));
    }
}
