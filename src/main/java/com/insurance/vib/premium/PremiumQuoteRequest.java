package com.insurance.vib.premium;

import com.insurance.vib.policy.PolicyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PremiumQuoteRequest(@NotNull Long policyholderId,
                                  @NotNull PolicyType type,
                                  @NotNull @DecimalMin("50000") @DecimalMax("10000000") BigDecimal sumInsured,
                                  @NotNull @Min(1) @Max(30) Integer termYears) {
}
