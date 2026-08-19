package com.coworking.roomops.backend.exception;

public class InvalidBookingPeriodException extends RuntimeException {

    public InvalidBookingPeriodException(String message) {
        super(message);
    }
}
