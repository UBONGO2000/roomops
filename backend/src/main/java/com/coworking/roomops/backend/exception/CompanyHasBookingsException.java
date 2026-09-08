package com.coworking.roomops.backend.exception;

public class CompanyHasBookingsException extends RuntimeException {

    public CompanyHasBookingsException(String message) {
        super(message);
    }
}