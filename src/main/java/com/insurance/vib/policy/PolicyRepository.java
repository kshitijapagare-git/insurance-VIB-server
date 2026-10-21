package com.insurance.vib.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByStatusAndEndDateBefore(PolicyStatus status, LocalDate endDate);

    List<Policy> findByStatusAndEndDateBetween(PolicyStatus status, LocalDate from, LocalDate to);
}
