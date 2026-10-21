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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PolicyholderRepository policyholderRepository;

    @Autowired
    private PolicyRepository policyRepository;

    private Long activePolicyholderId;
    private Long inactivePolicyholderId;

    @BeforeEach
    void setUp() {
        activePolicyholderId = policyholderRepository
                .save(new Policyholder(35, PolicyholderStatus.ACTIVE))
                .getId();
        inactivePolicyholderId = policyholderRepository
                .save(new Policyholder(35, PolicyholderStatus.INACTIVE))
                .getId();
    }

    @Test
    void createsPolicyWithCalculatedPremium() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "LIFE", "100000", 1, "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.policyholderId").value(activePolicyholderId.intValue()))
                .andExpect(jsonPath("$.type").value("LIFE"))
                .andExpect(jsonPath("$.basePremium").value(1000.00))
                .andExpect(jsonPath("$.ageLoading").value(100.00))
                .andExpect(jsonPath("$.gst").value(198.00))
                .andExpect(jsonPath("$.totalPremium").value(1298.00))
                .andReturn();

        Policy stored = storedPolicyOf(result);
        assertAmount(stored.getBasePremium(), new BigDecimal("1000.00"));
        assertAmount(stored.getAgeLoading(), new BigDecimal("100.00"));
        assertAmount(stored.getGst(), new BigDecimal("198.00"));
        assertAmount(stored.getTotalPremium(), new BigDecimal("1298.00"));
        assertThat(stored.getTermYears()).isEqualTo(1);
    }

    @Test
    void ignoresClientSuppliedPremiumAmounts() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "LIFE", "100000", 1,
                                ", \"basePremium\": 1, \"ageLoading\": 1, \"gst\": 1, \"totalPremium\": 1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.basePremium").value(1000.00))
                .andExpect(jsonPath("$.ageLoading").value(100.00))
                .andExpect(jsonPath("$.gst").value(198.00))
                .andExpect(jsonPath("$.totalPremium").value(1298.00))
                .andReturn();

        Policy stored = storedPolicyOf(result);
        assertAmount(stored.getBasePremium(), new BigDecimal("1000.00"));
        assertAmount(stored.getAgeLoading(), new BigDecimal("100.00"));
        assertAmount(stored.getGst(), new BigDecimal("198.00"));
        assertAmount(stored.getTotalPremium(), new BigDecimal("1298.00"));
    }

    @Test
    void returnsNotFoundForUnknownPolicyholderAndPersistsNothing() throws Exception {
        long policiesBefore = policyRepository.count();

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(999999L, "LIFE", "100000", 1, "")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("999999")))
                .andExpect(jsonPath("$.errors").isArray());

        assertThat(policyRepository.count()).isEqualTo(policiesBefore);
    }

    @Test
    void returnsUnprocessableEntityForInactivePolicyholderAndPersistsNothing() throws Exception {
        long policiesBefore = policyRepository.count();

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(inactivePolicyholderId, "LIFE", "100000", 1, "")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value(containsString("ACTIVE")))
                .andExpect(jsonPath("$.errors").isArray());

        assertThat(policyRepository.count()).isEqualTo(policiesBefore);
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "LIFE", "49999", 1, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value(containsString("sumInsured")));

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "LIFE", "100000", 31, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value(containsString("termYears")));

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "MOTOR", "100000", 2, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "DENTAL", "100000", 1, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    private Policy storedPolicyOf(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long policyId = json.get("id").asLong();
        return policyRepository.findById(policyId).orElseThrow();
    }

    private static void assertAmount(BigDecimal actual, BigDecimal expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.compareTo(expected))
                .as("expected %s but was %s", expected, actual)
                .isZero();
    }

    private static String body(Long policyholderId, String type, String sumInsured, int termYears,
                               String extraProperties) {
        return """
                {"policyholderId": %d, "type": "%s", "sumInsured": %s, "termYears": %d%s}
                """.formatted(policyholderId, type, sumInsured, termYears, extraProperties);
    }
}
