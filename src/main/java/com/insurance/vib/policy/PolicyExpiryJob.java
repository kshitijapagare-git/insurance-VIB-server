package com.insurance.vib.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class PolicyExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PolicyExpiryJob.class);

    private final PolicyRepository policyRepository;

    public PolicyExpiryJob(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Scheduled(cron = "${insurance.policy.expiry.cron:0 30 0 * * *}", zone = "UTC")
    @Transactional
    public int expirePolicies() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Policy> expired = policyRepository.findByStatusAndEndDateBefore(PolicyStatus.ACTIVE, today);
        expired.forEach(Policy::markExpired);
        policyRepository.saveAll(expired);
        log.info("Expired {} policies whose end date is before {}", expired.size(), today);
        return expired.size();
    }
}
