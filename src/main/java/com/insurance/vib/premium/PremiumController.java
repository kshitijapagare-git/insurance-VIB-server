package com.insurance.vib.premium;

import com.insurance.vib.policyholder.Policyholder;
import com.insurance.vib.policyholder.PolicyholderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/premiums")
public class PremiumController {

    private final PremiumCalculator premiumCalculator;
    private final PolicyholderService policyholderService;

    public PremiumController(PremiumCalculator premiumCalculator, PolicyholderService policyholderService) {
        this.premiumCalculator = premiumCalculator;
        this.policyholderService = policyholderService;
    }

    @PostMapping("/quote")
    public PremiumCalculation quote(@Valid @RequestBody PremiumQuoteRequest request) {
        Policyholder policyholder = policyholderService.requireActive(request.policyholderId());
        return premiumCalculator.calculate(
                request.type(),
                request.sumInsured(),
                request.termYears(),
                policyholder.getAge());
    }
}
