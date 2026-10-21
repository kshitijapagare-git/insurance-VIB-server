package com.insurance.vib.policy;

import java.math.BigDecimal;

public record PolicyResponse(Long id,
                             Long policyholderId,
                             PolicyType type,
                             BigDecimal sumInsured,
                             Integer termYears,
                             BigDecimal basePremium,
                             BigDecimal ageLoading,
                             BigDecimal gst,
                             BigDecimal totalPremium) {
}
