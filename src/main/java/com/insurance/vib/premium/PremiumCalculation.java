package com.insurance.vib.premium;

import java.math.BigDecimal;

public record PremiumCalculation(BigDecimal basePremium,
                                 BigDecimal ageLoading,
                                 BigDecimal gst,
                                 BigDecimal totalPremium) {
}
