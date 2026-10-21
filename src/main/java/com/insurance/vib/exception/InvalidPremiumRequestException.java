package com.insurance.vib.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidPremiumRequestException extends ResponseStatusException {

    public InvalidPremiumRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
