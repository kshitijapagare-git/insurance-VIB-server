package com.insurance.vib.policy;

import com.insurance.vib.claim.Claim;
import com.insurance.vib.claim.ClaimRepository;
import com.insurance.vib.claim.ClaimStatus;
import com.insurance.vib.policyholder.Policyholder;
import com.insurance.vib.policyholder.PolicyholderRepository;
import com.insurance.vib.policyholder.PolicyholderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PolicyCancellationTest {

    private static final BigDecimal BASE_PREMIUM = new BigDecimal("800.00");
    private static final BigDecimal AGE_LOADING = new BigDecimal("100.00");
    private static final BigDecimal GST = new BigDecimal("100.00");
    private static final BigDecimal TOTAL_PREMIUM = new BigDecimal("1000.00");
    private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
    private static final LocalDate END_DATE = START_DATE.plusYears(1);
    private static final String REASON = "Customer requested cancellation";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PolicyholderRepository policyholderRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private ClaimRepository claimRepository;

    private Policyholder policyholder;

    @BeforeEach
    void setUp() {
        claimRepository.deleteAll();
        policyRepository.deleteAll();
        policyholder = policyholderRepository.save(new Policyholder(35, PolicyholderStatus.ACTIVE));
    }

    @Test
    void cancelsAnActivePolicyAndReturnsTheFullPolicyRepresentation() throws Exception {
        Policy policy = activePolicy();
        LocalDate effectiveDate = START_DATE.plusMonths(6);
        Instant before = Instant.now();

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, effectiveDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(policy.getId().intValue()))
                .andExpect(jsonPath("$.policyholderId").value(policyholder.getId().intValue()))
                .andExpect(jsonPath("$.type").value("LIFE"))
                .andExpect(jsonPath("$.sumInsured").value(100000))
                .andExpect(jsonPath("$.termYears").value(1))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.startDate").value(START_DATE.toString()))
                .andExpect(jsonPath("$.endDate").value(END_DATE.toString()))
                .andExpect(jsonPath("$.basePremium").value(800.00))
                .andExpect(jsonPath("$.ageLoading").value(100.00))
                .andExpect(jsonPath("$.gst").value(100.00))
                .andExpect(jsonPath("$.totalPremium").value(1000.00))
                .andExpect(jsonPath("$.cancellationReason").value(REASON))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty())
                .andExpect(jsonPath("$.refundAmount").value(502.73));

        Policy stored = reload(policy);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(stored.getCancellationReason()).isEqualTo(REASON);
        assertThat(stored.getCancelledAt()).isNotNull();
        assertThat(stored.getCancelledAt()).isAfterOrEqualTo(before);
        assertThat(stored.getCancelledAt()).isBeforeOrEqualTo(Instant.now());
        assertAmount(stored.getRefundAmount(), new BigDecimal("502.73"));
        assertThat(stored.getRefundAmount().scale()).isEqualTo(2);
    }

    @Test
    void refundsTheWholeTotalPremiumWhenTheEffectiveDateIsTheStartDate() throws Exception {
        Policy policy = activePolicy();

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, START_DATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(1000.00));

        Policy stored = reload(policy);
        assertAmount(stored.getRefundAmount(), TOTAL_PREMIUM);
        assertThat(stored.getRefundAmount().scale()).isEqualTo(2);
    }

    @Test
    void refundsNothingWhenTheEffectiveDateIsTheEndDate() throws Exception {
        Policy policy = activePolicy();

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, END_DATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(0.00));

        Policy stored = reload(policy);
        assertAmount(stored.getRefundAmount(), BigDecimal.ZERO);
        assertThat(stored.getRefundAmount().scale()).isEqualTo(2);
    }

    @Test
    void refundsNothingWhenThePolicyHasAnApprovedClaim() throws Exception {
        Policy policy = activePolicy();
        claimRepository.save(new Claim(policy.getId(), ClaimStatus.APPROVED));

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, START_DATE.plusMonths(6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(0.00));

        Policy stored = reload(policy);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertAmount(stored.getRefundAmount(), BigDecimal.ZERO);
        assertThat(stored.getRefundAmount().scale()).isEqualTo(2);
    }

    @Test
    void appliesTheProRataRefundWhenTheOnlyClaimIsNotApproved() throws Exception {
        Policy policy = activePolicy();
        claimRepository.save(new Claim(policy.getId(), ClaimStatus.SUBMITTED));

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, START_DATE.plusMonths(6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(502.73));

        Policy stored = reload(policy);
        assertAmount(stored.getRefundAmount(), new BigDecimal("502.73"));
        assertThat(stored.getRefundAmount().scale()).isEqualTo(2);
    }

    @Test
    void rejectsCancellingAnExpiredPolicy() throws Exception {
        Policy policy = activePolicy();
        policy.markExpired();
        policyRepository.save(policy);

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, START_DATE.plusMonths(6))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("EXPIRED")))
                .andExpect(jsonPath("$.errors").isArray());

        Policy stored = reload(policy);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.EXPIRED);
        assertThat(stored.getCancellationReason()).isNull();
        assertThat(stored.getCancelledAt()).isNull();
        assertThat(stored.getRefundAmount()).isNull();
    }

    @Test
    void rejectsCancellingAPolicyThatIsAlreadyCancelled() throws Exception {
        Policy policy = activePolicy();
        Instant originalCancelledAt = Instant.now().minusSeconds(60);
        policy.cancel("Cancelled while still in force", originalCancelledAt, new BigDecimal("100.00"));
        policyRepository.save(policy);

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, START_DATE.plusMonths(6))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("CANCELLED")))
                .andExpect(jsonPath("$.errors").isArray());

        Policy stored = reload(policy);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(stored.getCancellationReason()).isEqualTo("Cancelled while still in force");
        assertThat(stored.getCancelledAt()).isNotNull();
        assertThat(stored.getCancelledAt().truncatedTo(ChronoUnit.MILLIS))
                .isEqualTo(originalCancelledAt.truncatedTo(ChronoUnit.MILLIS));
        assertAmount(stored.getRefundAmount(), new BigDecimal("100.00"));
    }

    @Test
    void rejectsAReasonShorterThanTenCharacters() throws Exception {
        Policy policy = activePolicy();

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("too short", START_DATE.plusMonths(6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value(containsString("reason")));

        Policy stored = reload(policy);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(stored.getCancellationReason()).isNull();
        assertThat(stored.getCancelledAt()).isNull();
        assertThat(stored.getRefundAmount()).isNull();
    }

    @Test
    void rejectsAReasonLongerThanFiveHundredCharacters() throws Exception {
        Policy policy = activePolicy();

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("x".repeat(501), START_DATE.plusMonths(6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value(containsString("reason")));

        Policy stored = reload(policy);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(stored.getCancellationReason()).isNull();
        assertThat(stored.getRefundAmount()).isNull();
    }

    @Test
    void rejectsAMissingReason() throws Exception {
        Policy policy = activePolicy();

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, START_DATE.plusMonths(6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value(containsString("reason")));

        Policy stored = reload(policy);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
    }

    @Test
    void rejectsAnEffectiveDateBeforeThePolicyStartDate() throws Exception {
        Policy policy = activePolicy();

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, START_DATE.minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString(START_DATE.toString())));

        Policy stored = reload(policy);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(stored.getCancellationReason()).isNull();
        assertThat(stored.getCancelledAt()).isNull();
        assertThat(stored.getRefundAmount()).isNull();
    }

    @Test
    void rejectsAnEffectiveDateAfterThePolicyEndDate() throws Exception {
        Policy policy = activePolicy();

        mockMvc.perform(post("/api/v1/policies/{id}/cancel", policy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, END_DATE.plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString(END_DATE.toString())));

        Policy stored = reload(policy);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(stored.getCancellationReason()).isNull();
        assertThat(stored.getRefundAmount()).isNull();
    }

    @Test
    void returnsNotFoundForAnUnknownPolicyIdentifier() throws Exception {
        mockMvc.perform(post("/api/v1/policies/{id}/cancel", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REASON, START_DATE.plusMonths(6))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("999999")))
                .andExpect(jsonPath("$.errors").isArray());
    }

    private Policy activePolicy() {
        return policyRepository.save(new Policy(
                policyholder,
                PolicyType.LIFE,
                new BigDecimal("100000"),
                1,
                START_DATE,
                BASE_PREMIUM,
                AGE_LOADING,
                GST,
                TOTAL_PREMIUM));
    }

    private Policy reload(Policy policy) {
        return policyRepository.findById(policy.getId()).orElseThrow();
    }

    private static void assertAmount(BigDecimal actual, BigDecimal expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.compareTo(expected))
                .as("expected %s but was %s", expected, actual)
                .isZero();
    }

    private static String body(String reason, LocalDate effectiveDate) {
        String reasonJson = reason == null ? "null" : "\"" + reason + "\"";
        return """
                {"reason": %s, "effectiveDate": "%s"}
                """.formatted(reasonJson, effectiveDate);
    }
}
