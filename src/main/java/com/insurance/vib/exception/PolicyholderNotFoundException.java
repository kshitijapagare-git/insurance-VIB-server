package com.insurance.vib.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PolicyholderNotFoundException extends ResponseStatusException {

    public PolicyholderNotFoundException(Long policyholderId) {
        super(HttpStatus.NOT_FOUND, "Policyholder " + policyholderId + " not found");
    }
}
