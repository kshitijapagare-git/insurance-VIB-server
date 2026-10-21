package com.insurance.vib.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.vib.policyholder.Policyholder;
import com.insurance.vib.policyholder.PolicyholderRepository;
import com.insurance.vib.policyholder.PolicyholderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PolicyCreationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PolicyholderRepository policyholderRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long activePolicyholderId;

    @BeforeEach
    void setUp() {
        activePolicyholderId = policyholderRepository
                .save(new Policyholder(35, PolicyholderStatus.ACTIVE))
                .getId();
    }

    @Test
    void createsActivePolicyWithEndDateDerivedFromTheTerm() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        MvcResult result = mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("LIFE", "100000", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startDate").value(today.toString()))
                .andExpect(jsonPath("$.endDate").value(today.plusYears(1).toString()))
                .andReturn();

        Policy stored = storedPolicyOf(result);
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(stored.getStartDate()).isEqualTo(today);
        assertThat(stored.getEndDate()).isEqualTo(today.plusYears(1));
    }

    @Test
    void addsTheWholeTermToTheStartDateToGetTheEndDate() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        MvcResult result = mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("HEALTH", "100000", 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startDate").value(today.toString()))
                .andExpect(jsonPath("$.endDate").value(today.plusYears(5).toString()))
                .andReturn();

        Policy stored = storedPolicyOf(result);
        assertThat(stored.getStartDate()).isEqualTo(today);
        assertThat(stored.getEndDate()).isEqualTo(today.plusYears(5));
    }

    @Test
    void defaultsPolicyStatusToActiveWhenTheColumnIsOmitted() {
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusYears(1);
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);

        Long policyId = insertPolicyWithoutStatus(startDate, endDate);

        Policy stored = policyRepository.findById(policyId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(stored.getStartDate()).isEqualTo(startDate);
        assertThat(stored.getEndDate()).isEqualTo(endDate);
    }

    @Test
    void markExpiredOnlyChangesActivePolicies() {
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusYears(1);
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);

        Policy cancelled = policyRepository
                .findById(insertPolicyWithStatus("CANCELLED", startDate, endDate))
                .orElseThrow();
        Policy active = policyRepository
                .findById(insertPolicyWithStatus("ACTIVE", startDate, endDate))
                .orElseThrow();

        cancelled.markExpired();
        active.markExpired();

        assertThat(cancelled.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(active.getStatus()).isEqualTo(PolicyStatus.EXPIRED);
    }

    private Policy storedPolicyOf(MvcResult result) throws Exception {
        long policyId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        return policyRepository.findById(policyId).orElseThrow();
    }

    private Long insertPolicyWithoutStatus(LocalDate startDate, LocalDate endDate) {
        jdbcTemplate.update("""
                insert into policies (policyholder_id, type, sum_insured, term_years, start_date, end_date,
                                      base_premium, age_loading, gst, total_premium)
                values (?, 'LIFE', 100000, 1, ?, ?, 1000.00, 100.00, 198.00, 1298.00)
                """, activePolicyholderId, startDate, endDate);
        return lastInsertedPolicyId();
    }

    private Long insertPolicyWithStatus(String status, LocalDate startDate, LocalDate endDate) {
        jdbcTemplate.update("""
                insert into policies (policyholder_id, type, sum_insured, term_years, status, start_date, end_date,
                                      base_premium, age_loading, gst, total_premium)
                values (?, 'LIFE', 100000, 1, ?, ?, ?, 1000.00, 100.00, 198.00, 1298.00)
                """, activePolicyholderId, status, startDate, endDate);
        return lastInsertedPolicyId();
    }

    private Long lastInsertedPolicyId() {
        return jdbcTemplate.queryForObject(
                "select max(id) from policies where policyholder_id = ?", Long.class, activePolicyholderId);
    }

    private String body(String type, String sumInsured, int termYears) {
        return """
                {"policyholderId": %d, "type": "%s", "sumInsured": %s, "termYears": %d}
                """.formatted(activePolicyholderId, type, sumInsured, termYears);
    }
}
