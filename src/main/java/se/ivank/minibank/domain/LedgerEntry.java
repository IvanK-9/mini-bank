package se.ivank.minibank.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** One line of an account's history. Entries are only ever added, never changed. */
@Entity
@Table(name = "ledger_entries", indexes = @Index(columnList = "accountId"))
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** The other account for transfers, otherwise null. */
    private Long relatedAccountId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected LedgerEntry() {
        // required by JPA
    }

    public LedgerEntry(Long accountId, EntryType type, BigDecimal amount, Long relatedAccountId) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.relatedAccountId = relatedAccountId;
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public EntryType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Long getRelatedAccountId() { return relatedAccountId; }
    public Instant getCreatedAt() { return createdAt; }
}
