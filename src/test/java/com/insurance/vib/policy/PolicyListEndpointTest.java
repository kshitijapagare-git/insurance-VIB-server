package com.insurance.vib.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.vib.policyholder.Policyholder;
import com.insurance.vib.policyholder.PolicyholderRepository;
import com.insurance.vib.policyholder.PolicyholderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PolicyListEndpointTest {

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

    private Policyholder policyholder;

    private Policy expiringToday;
    private Policy expiringOnTheUpperBoundary;
    private Policy expiringBeyondTheWindow;
    private Policy expiredByDateYesterday;
    private Policy expiredStatus;
    private Policy cancelledStatus;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
        policyholder = policyholderRepository.save(new Policyholder(35, PolicyholderStatus.ACTIVE));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        expiringToday = policyRepository.save(policyEndingOn(today));
        expiringOnTheUpperBoundary = policyRepository.save(policyEndingOn(today.plusDays(30)));
        expiringBeyondTheWindow = policyRepository.save(policyEndingOn(today.plusDays(31)));
        expiredByDateYesterday = policyRepository.save(policyEndingOn(today.minusDays(1)));

        Policy alreadyExpired = policyEndingOn(today.plusDays(5));
        alreadyExpired.markExpired();
        expiredStatus = policyRepository.save(alreadyExpired);

        cancelledStatus = policyRepository
                .findById(insertCancelledPolicy(today.plusDays(5)))
                .orElseThrow();
    }

    @Test
    void listsActivePoliciesExpiringFromTodayThroughTheEndOfTheWindow() throws Exception {
        List<Long> ids = idsOf(list("30"));

        assertThat(ids).containsExactlyInAnyOrder(expiringToday.getId(), expiringOnTheUpperBoundary.getId());
    }

    @Test
    void excludesPoliciesOutsideTheWindowAndPoliciesThatAreNotActive() throws Exception {
        List<Long> ids = idsOf(list("30"));

        assertThat(ids).doesNotContain(
                expiringBeyondTheWindow.getId(),
                expiredByDateYesterday.getId(),
                expiredStatus.getId(),
                cancelledStatus.getId());
    }

    @Test
    void rejectsExpiringWithinDaysOutsideTheAllowedRange() throws Exception {
        for (String value : List.of("0", "-1", "366", "1000")) {
            MvcResult result = mockMvc.perform(get("/api/v1/policies").param("expiringWithinDays", value))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors[0]").value(containsString("expiringWithinDays")))
                    .andReturn();

            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(json.isArray()).as("no policies are returned for %s", value).isFalse();
            assertThat(json.has("id")).as("no policy is returned for %s", value).isFalse();
        }
    }

    @Test
    void listsEveryPolicyWhenTheWindowIsNotGiven() throws Exception {
        List<Long> ids = idsOf(list(null));

        assertThat(ids).containsExactlyInAnyOrder(
                expiringToday.getId(),
                expiringOnTheUpperBoundary.getId(),
                expiringBeyondTheWindow.getId(),
                expiredByDateYesterday.getId(),
                expiredStatus.getId(),
                cancelledStatus.getId());
    }

    private JsonNode list(String expiringWithinDays) throws Exception {
        var request = get("/api/v1/policies");
        if (expiringWithinDays != null) {
            request = request.param("expiringWithinDays", expiringWithinDays);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static List<Long> idsOf(JsonNode json) {
        List<Long> ids = new ArrayList<>();
        json.forEach(node -> ids.add(node.get("id").asLong()));
        return ids;
    }

    private Policy policyEndingOn(LocalDate endDate) {
        return new Policy(policyholder, PolicyType.LIFE, new BigDecimal("100000"), 1, endDate.minusYears(1),
                new BigDecimal("1000.00"), new BigDecimal("100.00"), new BigDecimal("198.00"),
                new BigDecimal("1298.00"));
    }

    private Long insertCancelledPolicy(LocalDate endDate) {
        jdbcTemplate.update("""
                insert into policies (policyholder_id, type, sum_insured, term_years, status, start_date, end_date,
                                      base_premium, age_loading, gst, total_premium)
                values (?, 'LIFE', 100000, 1, 'CANCELLED', ?, ?, 1000.00, 100.00, 198.00, 1298.00)
                """, policyholder.getId(), endDate.minusYears(1), endDate);
        return jdbcTemplate.queryForObject(
                "select max(id) from policies where policyholder_id = ?", Long.class, policyholder.getId());
    }
}
