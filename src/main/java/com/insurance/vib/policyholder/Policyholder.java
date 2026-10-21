package com.insurance.vib.policyholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "policyholders")
public class Policyholder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyholderStatus status;

    protected Policyholder() {
    }

    public Policyholder(Integer age, PolicyholderStatus status) {
        this.age = age;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Integer getAge() {
        return age;
    }

    public PolicyholderStatus getStatus() {
        return status;
    }
}
