package io.sertaoBit.odontocore.crm.exception;

import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@NullMarked
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(NOT_FOUND)
                .body(ErrorResponse.of(NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ResourceAlreadyExistsException ex) {
        return ResponseEntity.status(CONFLICT)
                .body(ErrorResponse.of(CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(FORBIDDEN)
                .body(ErrorResponse.of(FORBIDDEN, ex.getMessage()));
    }

    @ExceptionHandler(DiscountApprovalRequiredException.class)
    public ResponseEntity<ErrorResponse> handleDiscountApproval(DiscountApprovalRequiredException ex) {
        return ResponseEntity.status(FORBIDDEN)
                .body(ErrorResponse.of(FORBIDDEN, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(UNPROCESSABLE_CONTENT, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleNotValid(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(BAD_REQUEST)
                .body(ErrorResponse.of(BAD_REQUEST,
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(e -> e.getField() + " : " + e.getDefaultMessage())
                                .collect(Collectors.joining(" , "))));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handlerNotValidated(ConstraintViolationException ex) {
        return ResponseEntity.status(BAD_REQUEST)
                .body(ErrorResponse.of(BAD_REQUEST, ex.getMessage()));
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handlerBadCredential(BadCredentialsException ex) {
        return ResponseEntity.status(UNAUTHORIZED)
                .body(ErrorResponse.of(UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(INTERNAL_SERVER_ERROR, ex.getMessage()));
    }


}