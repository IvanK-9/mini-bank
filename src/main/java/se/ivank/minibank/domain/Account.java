package se.ivank.minibank.domain;

import jakarta.persistence.*;
import se.ivank.minibank.exception.InsufficientFundsException;
import se.ivank.minibank.exception.InvalidOperationException;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Optimistic locking: a concurrent update of the same row fails instead of losing money. */
    @Version
    private Long version;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Account() {
        // required by JPA
    }

    public Account(String ownerName) {
        this.ownerName = ownerName;
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        balance = balance.subtract(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidOperationException("Amount must be positive");
        }
    }

    public Long getId() { return id; }
    public String getOwnerName() { return ownerName; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
}
