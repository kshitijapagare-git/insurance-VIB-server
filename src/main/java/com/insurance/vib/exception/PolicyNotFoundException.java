package com.insurance.vib.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PolicyNotFoundException extends ResponseStatusException {

    public PolicyNotFoundException(Long policyId) {
        super(HttpStatus.NOT_FOUND, "Policy " + policyId + " not found");
    }
}
