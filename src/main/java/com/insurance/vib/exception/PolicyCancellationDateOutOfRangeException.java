package com.insurance.vib.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

public class PolicyCancellationDateOutOfRangeException extends ResponseStatusException {

    public PolicyCancellationDateOutOfRangeException(LocalDate effectiveDate, LocalDate startDate, LocalDate endDate) {
        super(HttpStatus.BAD_REQUEST,
                "Effective date " + effectiveDate + " is outside the policy period " + startDate + " to " + endDate);
    }
}
