package se.ivank.minibank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ivank.minibank.domain.LedgerEntry;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByAccountIdOrderByIdDesc(Long accountId);
}
