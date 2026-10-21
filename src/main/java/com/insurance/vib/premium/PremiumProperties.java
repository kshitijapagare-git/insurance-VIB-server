package com.insurance.vib.premium;

import com.insurance.vib.policy.PolicyType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "insurance.premium")
public record PremiumProperties(BigDecimal gstRate,
                                Map<PolicyType, PremiumTypeRate> types,
                                List<AgeLoadingBand> ageLoadings) {
}
