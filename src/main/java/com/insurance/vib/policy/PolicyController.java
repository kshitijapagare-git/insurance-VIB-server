package com.insurance.vib.policy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody PolicyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(request));
    }

    @GetMapping
    public List<PolicyResponse> list(
            @RequestParam(name = "expiringWithinDays", required = false)
            @Min(1) @Max(365) Integer expiringWithinDays) {
        return policyService.list(expiringWithinDays);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PolicyResponse> cancel(@PathVariable("id") Long id,
                                                 @Valid @RequestBody PolicyCancellationRequest request) {
        return ResponseEntity.ok(policyService.cancel(id, request));
    }
}
