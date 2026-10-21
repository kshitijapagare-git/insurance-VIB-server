package com.insurance.vib.policy;

import com.insurance.vib.policyholder.Policyholder;
import com.insurance.vib.policyholder.PolicyholderService;
import com.insurance.vib.premium.PremiumCalculation;
import com.insurance.vib.premium.PremiumCalculator;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyholderService policyholderService;
    private final PremiumCalculator premiumCalculator;

    public PolicyService(PolicyRepository policyRepository,
                         PolicyholderService policyholderService,
                         PremiumCalculator premiumCalculator) {
        this.policyRepository = policyRepository;
        this.policyholderService = policyholderService;
        this.premiumCalculator = premiumCalculator;
    }

    public PolicyResponse create(PolicyCreateRequest request) {
        Policyholder policyholder = policyholderService.requireActive(request.policyholderId());
        PremiumCalculation calculation = premiumCalculator.calculate(
                request.type(),
                request.sumInsured(),
                request.termYears(),
                policyholder.getAge());

        Policy policy = new Policy(
                policyholder,
                request.type(),
                request.sumInsured(),
                request.termYears(),
                calculation.basePremium(),
                calculation.ageLoading(),
                calculation.gst(),
                calculation.totalPremium());

        Policy saved = policyRepository.save(policy);
        return new PolicyResponse(
                saved.getId(),
                saved.getPolicyholder().getId(),
                saved.getType(),
                saved.getSumInsured(),
                saved.getTermYears(),
                saved.getBasePremium(),
                saved.getAgeLoading(),
                saved.getGst(),
                saved.getTotalPremium());
    }
}
