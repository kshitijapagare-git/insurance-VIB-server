package com.insurance.vib.exception;

import com.insurance.vib.policy.PolicyStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PolicyNotCancellableException extends ResponseStatusException {

    public PolicyNotCancellableException(Long policyId, PolicyStatus status) {
        super(HttpStatus.CONFLICT, "Policy " + policyId + " cannot be cancelled in status " + status);
    }
}
