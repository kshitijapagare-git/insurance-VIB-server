package com.insurance.vib.premium;

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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PremiumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PolicyholderRepository policyholderRepository;

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
    void quotesHeathPremiumForActivePolicyholder() throws Exception {
        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "HEALTH", "100000", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePremium").value(2000.00))
                .andExpect(jsonPath("$.ageLoading").value(200.00))
                .andExpect(jsonPath("$.gst").value(396.00))
                .andExpect(jsonPath("$.totalPremium").value(2596.00))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void suppressesAgeLoadingForMotor() throws Exception {
        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "MOTOR", "100000", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePremium").value(3000.00))
                .andExpect(jsonPath("$.ageLoading").value(0.00))
                .andExpect(jsonPath("$.gst").value(540.00))
                .andExpect(jsonPath("$.totalPremium").value(3540.00));
    }

    @Test
    void returnsNotFoundForUnknownPolicyholder() throws Exception {
        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(999999L, "HEALTH", "100000", 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("999999")))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void returnsUnprocessableEntityForInactivePolicyholder() throws Exception {
        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(inactivePolicyholderId, "HEALTH", "100000", 1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value(containsString("ACTIVE")))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void rejectsSumInsuredOutsideTheAllowedRange() throws Exception {
        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "HEALTH", "49999.99", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value(containsString("sumInsured")));

        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "HEALTH", "10000000.01", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value(containsString("sumInsured")));
    }

    @Test
    void rejectsTermYearsOutsideTheAllowedRange() throws Exception {
        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "HEALTH", "100000", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value(containsString("termYears")));

        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "HEALTH", "100000", 31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value(containsString("termYears")));
    }

    @Test
    void rejectsMotorWithATermOtherThanOneYear() throws Exception {
        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "MOTOR", "100000", 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsUnknownPolicyType() throws Exception {
        mockMvc.perform(post("/api/v1/premiums/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(activePolicyholderId, "DENTAL", "100000", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    private static String body(Long policyholderId, String type, String sumInsured, int termYears) {
        return """
                {"policyholderId": %d, "type": "%s", "sumInsured": %s, "termYears": %d}
                """.formatted(policyholderId, type, sumInsured, termYears);
    }
}
