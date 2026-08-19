package com.coworking.roomops.backend.exception;

public class EmployeeHasBookingsException extends RuntimeException {

    public EmployeeHasBookingsException(String message) {
        super(message);
    }
}
