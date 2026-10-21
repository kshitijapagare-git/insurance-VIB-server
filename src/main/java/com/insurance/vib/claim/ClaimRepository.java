package com.insurance.vib.claim;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    boolean existsByPolicyIdAndStatus(Long policyId, ClaimStatus status);
}
