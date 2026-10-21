package com.insurance.vib.premium;

import com.insurance.vib.exception.InvalidPremiumRequestException;
import com.insurance.vib.policy.PolicyType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PremiumCalculator {

    private static final int AMOUNT_SCALE = 2;
    private static final int MOTOR_TERM_YEARS = 1;

    private final PremiumProperties premiumProperties;

    public PremiumCalculator(PremiumProperties premiumProperties) {
        this.premiumProperties = premiumProperties;
    }

    public PremiumCalculation calculate(PolicyType type, BigDecimal sumInsured, int termYears, int age) {
        if (type == null) {
            throw new InvalidPremiumRequestException("Policy type is required");
        }
        if (type == PolicyType.MOTOR && termYears != MOTOR_TERM_YEARS) {
            throw new InvalidPremiumRequestException("MOTOR policies allow only " + MOTOR_TERM_YEARS + " year term");
        }
        PremiumTypeRate typeRate = premiumProperties.types().get(type);
        if (typeRate == null) {
            throw new InvalidPremiumRequestException("No premium rate configured for policy type " + type);
        }

        BigDecimal basePremium = typeRate.baseRate()
                .multiply(sumInsured)
                .multiply(BigDecimal.valueOf(termYears))
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        BigDecimal ageLoading = typeRate.ageLoadingApplies()
                ? basePremium.multiply(loadingFor(age)).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        BigDecimal gst = premiumProperties.gstRate()
                .multiply(basePremium.add(ageLoading))
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        BigDecimal totalPremium = basePremium.add(ageLoading).add(gst)
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        return new PremiumCalculation(basePremium, ageLoading, gst, totalPremium);
    }

    private BigDecimal loadingFor(int age) {
        return premiumProperties.ageLoadings().stream()
                .filter(band -> band.maxAge() >= age)
                .map(AgeLoadingBand::loading)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
