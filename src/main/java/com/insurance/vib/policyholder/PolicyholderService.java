package com.insurance.vib.policyholder;

import com.insurance.vib.exception.PolicyholderNotActiveException;
import com.insurance.vib.exception.PolicyholderNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PolicyholderService {

    private final PolicyholderRepository policyholderRepository;

    public PolicyholderService(PolicyholderRepository policyholderRepository) {
        this.policyholderRepository = policyholderRepository;
    }

    public Policyholder requireActive(Long policyholderId) {
        Policyholder policyholder = policyholderRepository.findById(policyholderId)
                .orElseThrow(() -> new PolicyholderNotFoundException(policyholderId));
        if (policyholder.getStatus() != PolicyholderStatus.ACTIVE) {
            throw new PolicyholderNotActiveException(policyholderId);
        }
        return policyholder;
    }
}
