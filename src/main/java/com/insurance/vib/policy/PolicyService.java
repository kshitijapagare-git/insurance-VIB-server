package com.insurance.vib.policy;

import com.insurance.vib.policyholder.Policyholder;
import com.insurance.vib.policyholder.PolicyholderService;
import com.insurance.vib.premium.PremiumCalculation;
import com.insurance.vib.premium.PremiumCalculator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

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
                LocalDate.now(ZoneOffset.UTC),
                calculation.basePremium(),
                calculation.ageLoading(),
                calculation.gst(),
                calculation.totalPremium());

        Policy saved = policyRepository.save(policy);
        return toResponse(saved);
    }

    public List<PolicyResponse> list(Integer expiringWithinDays) {
        if (expiringWithinDays == null) {
            return policyRepository.findAll().stream().map(PolicyService::toResponse).toList();
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return policyRepository
                .findByStatusAndEndDateBetween(PolicyStatus.ACTIVE, today, today.plusDays(expiringWithinDays))
                .stream()
                .map(PolicyService::toResponse)
                .toList();
    }

    private static PolicyResponse toResponse(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getPolicyholder().getId(),
                policy.getType(),
                policy.getSumInsured(),
                policy.getTermYears(),
                policy.getStatus(),
                policy.getStartDate(),
                policy.getEndDate(),
                policy.getBasePremium(),
                policy.getAgeLoading(),
                policy.getGst(),
                policy.getTotalPremium());
    }
}
