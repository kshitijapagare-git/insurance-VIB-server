package com.insurance.vib.premium;

import com.insurance.vib.exception.InvalidPremiumRequestException;
import com.insurance.vib.policy.PolicyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PremiumCalculatorTest {

    private static final BigDecimal SUM_INSURED = new BigDecimal("100000");

    @Autowired
    private PremiumCalculator premiumCalculator;

    @ParameterizedTest(name = "{0} age {1} -> {3} loading")
    @CsvSource({
            // type, age, basePremium, ageLoading, gst, totalPremium
            "HEALTH, 25, 2000.00, 0.00, 360.00, 2360.00",
            "HEALTH, 35, 2000.00, 200.00, 396.00, 2596.00",
            "HEALTH, 50, 2000.00, 500.00, 450.00, 2950.00",
            "HEALTH, 65, 2000.00, 1000.00, 540.00, 3540.00",
            "LIFE, 25, 1000.00, 0.00, 180.00, 1180.00",
            "LIFE, 35, 1000.00, 100.00, 198.00, 1298.00",
            "LIFE, 50, 1000.00, 250.00, 225.00, 1475.00",
            "LIFE, 65, 1000.00, 500.00, 270.00, 1770.00",
            "MOTOR, 25, 3000.00, 0.00, 540.00, 3540.00",
            "MOTOR, 35, 3000.00, 0.00, 540.00, 3540.00",
            "MOTOR, 50, 3000.00, 0.00, 540.00, 3540.00",
            "MOTOR, 65, 3000.00, 0.00, 540.00, 3540.00"
    })
    void calculatesEveryPolicyTypeAndAgeBand(PolicyType type, int age,
                                             BigDecimal basePremium, BigDecimal ageLoading,
                                             BigDecimal gst, BigDecimal totalPremium) {
        PremiumCalculation calculation = premiumCalculator.calculate(type, SUM_INSURED, 1, age);

        assertAmount(calculation.basePremium(), basePremium);
        assertAmount(calculation.ageLoading(), ageLoading);
        assertAmount(calculation.gst(), gst);
        assertAmount(calculation.totalPremium(), totalPremium);
    }

    @ParameterizedTest(name = "HEALTH age {0} -> loading {1}")
    @CsvSource({
            "25, 0.00",
            "29, 0.00",
            "30, 200.00",
            "45, 200.00",
            "46, 500.00",
            "60, 500.00",
            "61, 1000.00",
            "70, 1000.00"
    })
    void appliesTheAgeBandBoundariesOnHealth(int age, BigDecimal expectedAgeLoading) {
        PremiumCalculation calculation = premiumCalculator.calculate(PolicyType.HEALTH, SUM_INSURED, 1, age);

        assertAmount(calculation.ageLoading(), expectedAgeLoading);
    }

    @Test
    void neverAppliesAgeLoadingToMotor() {
        PremiumCalculation calculation = premiumCalculator.calculate(PolicyType.MOTOR, SUM_INSURED, 1, 65);

        assertAmount(calculation.basePremium(), new BigDecimal("3000.00"));
        assertAmount(calculation.ageLoading(), new BigDecimal("0.00"));
        assertAmount(calculation.gst(), new BigDecimal("540.00"));
        assertAmount(calculation.totalPremium(), new BigDecimal("3540.00"));
    }

    @Test
    void multipliesBasePremiumAcrossTheWholeTerm() {
        PremiumCalculation calculation = premiumCalculator.calculate(PolicyType.HEALTH, SUM_INSURED, 5, 25);

        assertAmount(calculation.basePremium(), new BigDecimal("10000.00"));
        assertAmount(calculation.ageLoading(), new BigDecimal("0.00"));
        assertAmount(calculation.gst(), new BigDecimal("1800.00"));
        assertAmount(calculation.totalPremium(), new BigDecimal("11800.00"));
    }

    @Test
    void roundsEveryAmountHalfUpToTwoDecimals() {
        PremiumCalculation calculation = premiumCalculator.calculate(
                PolicyType.HEALTH, new BigDecimal("55555.55"), 1, 46);

        assertAmount(calculation.basePremium(), new BigDecimal("1111.11"));
        assertAmount(calculation.ageLoading(), new BigDecimal("277.78"));
        assertAmount(calculation.gst(), new BigDecimal("250.00"));
        assertAmount(calculation.totalPremium(), new BigDecimal("1638.89"));

        assertThat(calculation.basePremium().scale()).isEqualTo(2);
        assertThat(calculation.ageLoading().scale()).isEqualTo(2);
        assertThat(calculation.gst().scale()).isEqualTo(2);
        assertThat(calculation.totalPremium().scale()).isEqualTo(2);
    }

    @Test
    void rejectsMotorWithATermOtherThanOneYear() {
        assertThatThrownBy(() -> premiumCalculator.calculate(PolicyType.MOTOR, SUM_INSURED, 2, 40))
                .isInstanceOf(InvalidPremiumRequestException.class);
    }

    private static void assertAmount(BigDecimal actual, BigDecimal expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.compareTo(expected))
                .as("expected %s but was %s", expected, actual)
                .isZero();
    }
}
