package com.insurance.vib.claim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ClaimRepositoryTest {

    @Autowired
    private ClaimRepository claimRepository;

    @BeforeEach
    void setUp() {
        claimRepository.deleteAll();
    }

    @Test
    void findsAnApprovedClaimForThePolicy() {
        claimRepository.save(new Claim(42L, ClaimStatus.APPROVED));

        assertThat(claimRepository.existsByPolicyIdAndStatus(42L, ClaimStatus.APPROVED)).isTrue();
    }

    @Test
    void reportsNoApprovedClaimForAPolicyWithoutClaims() {
        claimRepository.save(new Claim(42L, ClaimStatus.APPROVED));

        assertThat(claimRepository.existsByPolicyIdAndStatus(43L, ClaimStatus.APPROVED)).isFalse();
    }

    @Test
    void reportsNoApprovedClaimWhenTheOnlyClaimsAreNotApproved() {
        claimRepository.save(new Claim(42L, ClaimStatus.SUBMITTED));
        claimRepository.save(new Claim(42L, ClaimStatus.REJECTED));

        assertThat(claimRepository.existsByPolicyIdAndStatus(42L, ClaimStatus.APPROVED)).isFalse();
    }
}
