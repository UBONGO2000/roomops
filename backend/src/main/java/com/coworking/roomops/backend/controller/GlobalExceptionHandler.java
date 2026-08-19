package com.coworking.roomops.backend.controller;

import com.coworking.roomops.backend.exception.BookingConflictException;
import com.coworking.roomops.backend.exception.EmployeeHasBookingsException;
import com.coworking.roomops.backend.exception.InvalidBookingPeriodException;
import com.coworking.roomops.backend.exception.OptimisticLockConflictException;
import com.coworking.roomops.backend.model.ErrorResponse;
import com.coworking.roomops.backend.model.ErrorResponseDetailsInner;
import com.coworking.roomops.backend.security.InvalidTokenException;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("INVALID_CREDENTIALS", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("INVALID_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(EmployeeHasBookingsException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeHasBookings(EmployeeHasBookingsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody("EMPLOYEE_HAS_BOOKINGS", ex.getMessage()));
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ErrorResponse> handleBookingConflict(BookingConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody("BOOKING_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler({OptimisticLockConflictException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLockConflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        errorBody(
                                "OPTIMISTIC_LOCK_CONFLICT",
                                "La ressource a été modifiée depuis votre dernière lecture, merci de recharger"));
    }

    @ExceptionHandler(InvalidBookingPeriodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBookingPeriod(InvalidBookingPeriodException ex) {
        return ResponseEntity.badRequest().body(errorBody("INVALID_PERIOD", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        ErrorResponse body = errorBody("VALIDATION_ERROR", "Données invalides");
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(
                        fieldError ->
                                body.addDetailsItem(
                                        new ErrorResponseDetailsInner()
                                                .field(fieldError.getField())
                                                .message(fieldError.getDefaultMessage())));
        return ResponseEntity.badRequest().body(body);
    }

    private ErrorResponse errorBody(String code, String message) {
        return new ErrorResponse().code(code).message(message).timestamp(OffsetDateTime.now());
    }
}
