package com.reservo.controller.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Error response wrapper")
public record DTOResponseError(
    @Schema(description = "Timestamp of the error", example = "2026-04-22T14:30:00")
    LocalDateTime timestamp,
    
    @Schema(description = "HTTP status code", example = "400")
    int status,
    
    @Schema(description = "Error type", example = "BAD_REQUEST")
    String error,
    
    @Schema(description = "Detailed error message", example = "Email already registered")
    String message,
    
    @Schema(description = "Request path", example = "/api/usuarios/register")
    String path
) {
    public static DTOResponseError of(int status, String error, String message, String path) {
        return new DTOResponseError(LocalDateTime.now(), status, error, message, path);
    }
}
