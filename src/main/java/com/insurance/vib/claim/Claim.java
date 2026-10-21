package com.insurance.vib.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    protected Claim() {
    }

    public Claim(Long policyId, ClaimStatus status) {
        this.policyId = policyId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public ClaimStatus getStatus() {
        return status;
    }
}
