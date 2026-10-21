package com.insurance.vib.policy;

import com.insurance.vib.claim.ClaimRepository;
import com.insurance.vib.claim.ClaimStatus;
import com.insurance.vib.exception.PolicyCancellationDateOutOfRangeException;
import com.insurance.vib.exception.PolicyNotCancellableException;
import com.insurance.vib.exception.PolicyNotFoundException;
import com.insurance.vib.policyholder.Policyholder;
import com.insurance.vib.policyholder.PolicyholderService;
import com.insurance.vib.premium.PremiumCalculation;
import com.insurance.vib.premium.PremiumCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PolicyService {

    private static final int REFUND_SCALE = 2;

    private final PolicyRepository policyRepository;
    private final PolicyholderService policyholderService;
    private final PremiumCalculator premiumCalculator;
    private final ClaimRepository claimRepository;

    public PolicyService(PolicyRepository policyRepository,
                         PolicyholderService policyholderService,
                         PremiumCalculator premiumCalculator,
                         ClaimRepository claimRepository) {
        this.policyRepository = policyRepository;
        this.policyholderService = policyholderService;
        this.premiumCalculator = premiumCalculator;
        this.claimRepository = claimRepository;
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

    @Transactional
    public PolicyResponse cancel(Long policyId, PolicyCancellationRequest request) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new PolicyNotCancellableException(policyId, policy.getStatus());
        }
        LocalDate effectiveDate = request.effectiveDate();
        if (effectiveDate.isBefore(policy.getStartDate()) || effectiveDate.isAfter(policy.getEndDate())) {
            throw new PolicyCancellationDateOutOfRangeException(
                    effectiveDate, policy.getStartDate(), policy.getEndDate());
        }

        BigDecimal refundAmount = calculateRefund(policy, effectiveDate);
        policy.cancel(request.reason(), Instant.now(), refundAmount);
        Policy saved = policyRepository.save(policy);
        return toResponse(saved);
    }

    private BigDecimal calculateRefund(Policy policy, LocalDate effectiveDate) {
        if (claimRepository.existsByPolicyIdAndStatus(policy.getId(), ClaimStatus.APPROVED)) {
            return BigDecimal.ZERO.setScale(REFUND_SCALE, RoundingMode.HALF_UP);
        }
        long totalDays = ChronoUnit.DAYS.between(policy.getStartDate(), policy.getEndDate());
        if (totalDays <= 0) {
            return BigDecimal.ZERO.setScale(REFUND_SCALE, RoundingMode.HALF_UP);
        }
        long unusedDays = ChronoUnit.DAYS.between(effectiveDate, policy.getEndDate());
        long billableUnusedDays = Math.min(Math.max(unusedDays, 0), totalDays);
        return policy.getTotalPremium()
                .multiply(BigDecimal.valueOf(billableUnusedDays))
                .divide(BigDecimal.valueOf(totalDays), REFUND_SCALE, RoundingMode.HALF_UP);
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
                policy.getTotalPremium(),
                policy.getCancellationReason(),
                policy.getCancelledAt(),
                policy.getRefundAmount());
    }
}
