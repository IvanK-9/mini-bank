package se.ivank.minibank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ivank.minibank.domain.Transfer;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
