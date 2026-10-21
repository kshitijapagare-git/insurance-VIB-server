package com.insurance.vib.policy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyResponse(Long id,
                             Long policyholderId,
                             PolicyType type,
                             BigDecimal sumInsured,
                             Integer termYears,
                             PolicyStatus status,
                             LocalDate startDate,
                             LocalDate endDate,
                             BigDecimal basePremium,
                             BigDecimal ageLoading,
                             BigDecimal gst,
                             BigDecimal totalPremium) {
}
