package com.insurance.vib.policy;

import com.insurance.vib.policyholder.Policyholder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policyholder_id")
    private Policyholder policyholder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType type;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sumInsured;

    @Column(nullable = false)
    private Integer termYears;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(32) default 'ACTIVE'")
    private PolicyStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal basePremium;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ageLoading;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal gst;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPremium;

    protected Policy() {
    }

    public Policy(Policyholder policyholder, PolicyType type, BigDecimal sumInsured, Integer termYears,
                  LocalDate startDate,
                  BigDecimal basePremium, BigDecimal ageLoading, BigDecimal gst, BigDecimal totalPremium) {
        this.policyholder = policyholder;
        this.type = type;
        this.sumInsured = sumInsured;
        this.termYears = termYears;
        this.status = PolicyStatus.ACTIVE;
        this.startDate = startDate;
        this.endDate = startDate.plusYears(termYears);
        this.basePremium = basePremium;
        this.ageLoading = ageLoading;
        this.gst = gst;
        this.totalPremium = totalPremium;
    }

    public Long getId() {
        return id;
    }

    public Policyholder getPolicyholder() {
        return policyholder;
    }

    public PolicyType getType() {
        return type;
    }

    public BigDecimal getSumInsured() {
        return sumInsured;
    }

    public Integer getTermYears() {
        return termYears;
    }

    public BigDecimal getBasePremium() {
        return basePremium;
    }

    public BigDecimal getAgeLoading() {
        return ageLoading;
    }

    public BigDecimal getGst() {
        return gst;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void markExpired() {
        if (status == PolicyStatus.ACTIVE) {
            status = PolicyStatus.EXPIRED;
        }
    }
}
