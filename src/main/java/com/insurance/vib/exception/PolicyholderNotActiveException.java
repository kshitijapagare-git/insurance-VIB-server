package com.insurance.vib.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PolicyholderNotActiveException extends ResponseStatusException {

    public PolicyholderNotActiveException(Long policyholderId) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Policyholder " + policyholderId + " is not ACTIVE");
    }
}
