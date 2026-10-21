package com.insurance.vib.premium;

import java.math.BigDecimal;

public record PremiumTypeRate(BigDecimal baseRate, boolean ageLoadingApplies) {
}
