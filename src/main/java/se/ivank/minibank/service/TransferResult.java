package se.ivank.minibank.service;

import se.ivank.minibank.domain.Transfer;

/** replayed = true when the same Idempotency-Key was already processed. */
public record TransferResult(Transfer transfer, boolean replayed) {
}
