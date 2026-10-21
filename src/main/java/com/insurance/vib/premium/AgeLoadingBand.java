package com.insurance.vib.premium;

import java.math.BigDecimal;

public record AgeLoadingBand(int maxAge, BigDecimal loading) {
}
