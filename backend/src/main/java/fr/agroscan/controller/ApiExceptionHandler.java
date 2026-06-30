package fr.agroscan.controller;

import fr.agroscan.service.CurrentPasswordInvalidException;
import fr.agroscan.service.AdminOperationException;
import fr.agroscan.service.AnalysisUnavailableException;
import fr.agroscan.service.EmailAlreadyUsedException;
import fr.agroscan.service.InvalidCredentialsException;
import fr.agroscan.service.InvalidImageException;
import fr.agroscan.service.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError emailAlreadyUsed(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", "Cette adresse e-mail est déjà utilisée", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError dataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "La modification entre en conflit avec une donnée existante", request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ApiError invalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), request);
    }

    @ExceptionHandler(CurrentPasswordInvalidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError currentPasswordInvalid(CurrentPasswordInvalidException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_INVALID", exception.getMessage(), request);
    }

    @ExceptionHandler(AdminOperationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError adminOperationInvalid(AdminOperationException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "ADMIN_OPERATION_INVALID", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidImageException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalidImage(InvalidImageException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiError notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(AnalysisUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ApiError analysisUnavailable(AnalysisUnavailableException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, "ANALYSIS_UNAVAILABLE", exception.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalidParameter(ConstraintViolationException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Un paramètre de recherche est invalide", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalidRequest(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(field -> new FieldError(field.getField(), field.getDefaultMessage()))
                .toList();
        return new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Certaines informations sont invalides",
                request.getRequestURI(),
                fields
        );
    }

    private ApiError error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI(), List.of());
    }

    record ApiError(
            Instant timestamp,
            int status,
            String code,
            String message,
            String path,
            List<FieldError> fieldErrors
    ) {
    }

    record FieldError(String field, String message) {
    }
}
