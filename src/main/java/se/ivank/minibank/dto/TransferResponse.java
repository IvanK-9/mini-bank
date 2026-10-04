package se.ivank.minibank.dto;

import se.ivank.minibank.domain.Transfer;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(Long id, Long fromAccountId, Long toAccountId,
                               BigDecimal amount, Instant createdAt) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(t.getId(), t.getFromAccountId(), t.getToAccountId(),
                t.getAmount(), t.getCreatedAt());
    }
}
