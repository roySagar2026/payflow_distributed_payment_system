package com.payflow.payflow.repository;

import com.payflow.payflow.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
}