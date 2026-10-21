package com.insurance.vib.policy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PolicyCancellationRequest(@NotBlank @Size(min = 10, max = 500) String reason,
                                        @NotNull LocalDate effectiveDate) {
}
