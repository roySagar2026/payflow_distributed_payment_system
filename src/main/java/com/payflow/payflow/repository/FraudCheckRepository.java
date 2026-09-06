package com.payflow.payflow.repository;

import com.payflow.payflow.model.FraudCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FraudCheckRepository extends JpaRepository<FraudCheck, UUID> {
}