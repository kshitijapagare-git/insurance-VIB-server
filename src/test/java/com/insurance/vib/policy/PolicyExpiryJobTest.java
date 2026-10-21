package com.insurance.vib.policy;

import com.insurance.vib.policyholder.Policyholder;
import com.insurance.vib.policyholder.PolicyholderRepository;
import com.insurance.vib.policyholder.PolicyholderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class PolicyExpiryJobTest {

    @Autowired
    private PolicyExpiryJob policyExpiryJob;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyholderRepository policyholderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    private Policyholder policyholder;

    private Policy pastDueActive;
    private Policy alsoPastDueActive;
    private Policy dueTodayActive;
    private Policy futureActive;
    private Policy pastDueExpired;
    private Policy pastDueCancelled;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
        policyholder = policyholderRepository.save(new Policyholder(35, PolicyholderStatus.ACTIVE));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        pastDueActive = policyRepository.save(policy(today.minusYears(1).minusDays(1), 1));
        alsoPastDueActive = policyRepository.save(policy(today.minusYears(2), 1));
        dueTodayActive = policyRepository.save(policy(today.minusYears(1), 1));
        futureActive = policyRepository.save(policy(today, 1));

        Policy alreadyExpired = policy(today.minusYears(1).minusDays(1), 1);
        alreadyExpired.markExpired();
        pastDueExpired = policyRepository.save(alreadyExpired);

        pastDueCancelled = policyRepository.findById(
                        insertPolicy("CANCELLED", today.minusYears(1).minusDays(1), today.minusDays(1)))
                .orElseThrow();
    }

    @Test
    void expiresEveryActivePolicyWhoseEndDateHasPassed(CapturedOutput output) {
        int changed = policyExpiryJob.expirePolicies();

        assertThat(changed).isEqualTo(2);
        assertThat(output).contains("Expired 2 policies");
        assertThat(statusOf(pastDueActive)).isEqualTo(PolicyStatus.EXPIRED);
        assertThat(statusOf(alsoPastDueActive)).isEqualTo(PolicyStatus.EXPIRED);
    }

    @Test
    void leavesPoliciesThatAreNotOverdueActivePoliciesUntouched(CapturedOutput output) {
        policyExpiryJob.expirePolicies();

        assertThat(statusOf(dueTodayActive)).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(statusOf(futureActive)).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(statusOf(pastDueCancelled)).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(statusOf(pastDueExpired)).isEqualTo(PolicyStatus.EXPIRED);
    }

    @Test
    void isSafeToRunMoreThanOnce(CapturedOutput output) {
        assertThat(policyExpiryJob.expirePolicies()).isEqualTo(2);
        assertThat(statusOf(pastDueActive)).isEqualTo(PolicyStatus.EXPIRED);
        assertThat(statusOf(alsoPastDueActive)).isEqualTo(PolicyStatus.EXPIRED);

        int changedOnSecondRun = policyExpiryJob.expirePolicies();

        assertThat(changedOnSecondRun).isZero();
        assertThat(output).contains("Expired 0 policies");
        assertThat(statusOf(pastDueActive)).isEqualTo(PolicyStatus.EXPIRED);
        assertThat(statusOf(alsoPastDueActive)).isEqualTo(PolicyStatus.EXPIRED);
        assertThat(statusOf(dueTodayActive)).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(statusOf(futureActive)).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(statusOf(pastDueCancelled)).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(statusOf(pastDueExpired)).isEqualTo(PolicyStatus.EXPIRED);
    }

    @Test
    void schedulesTheJobOnTheDefaultCron() {
        assertThat(scheduledCronExpressions()).contains("0 30 0 * * *");
    }

    private List<String> scheduledCronExpressions() {
        return scheduledTaskHolder.getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .map(CronTask::getExpression)
                .toList();
    }

    private PolicyStatus statusOf(Policy policy) {
        return policyRepository.findById(policy.getId()).orElseThrow().getStatus();
    }

    private Policy policy(LocalDate startDate, int termYears) {
        return new Policy(policyholder, PolicyType.LIFE, new BigDecimal("100000"), termYears, startDate,
                new BigDecimal("1000.00"), new BigDecimal("100.00"), new BigDecimal("198.00"),
                new BigDecimal("1298.00"));
    }

    private Long insertPolicy(String status, LocalDate startDate, LocalDate endDate) {
        jdbcTemplate.update("""
                insert into policies (policyholder_id, type, sum_insured, term_years, status, start_date, end_date,
                                      base_premium, age_loading, gst, total_premium)
                values (?, 'LIFE', 100000, 1, ?, ?, ?, 1000.00, 100.00, 198.00, 1298.00)
                """, policyholder.getId(), status, startDate, endDate);
        return jdbcTemplate.queryForObject(
                "select max(id) from policies where policyholder_id = ?", Long.class, policyholder.getId());
    }
}

@SpringBootTest(properties = "insurance.policy.expiry.cron=0 15 3 * * *")
class PolicyExpiryJobCronPropertyTest {

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    @Test
    void usesTheCronExpressionFromTheConfigurationProperty() {
        List<String> cronExpressions = scheduledTaskHolder.getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .map(CronTask::getExpression)
                .toList();

        assertThat(cronExpressions).contains("0 15 3 * * *");
    }
}
