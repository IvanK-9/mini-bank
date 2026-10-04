package se.ivank.minibank.dto;

import se.ivank.minibank.domain.EntryType;
import se.ivank.minibank.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;

public record EntryResponse(Long id, EntryType type, BigDecimal amount,
                            Long relatedAccountId, Instant createdAt) {
    public static EntryResponse from(LedgerEntry e) {
        return new EntryResponse(e.getId(), e.getType(), e.getAmount(),
                e.getRelatedAccountId(), e.getCreatedAt());
    }
}
