package se.ivank.minibank.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ivank.minibank.domain.Account;
import se.ivank.minibank.domain.EntryType;
import se.ivank.minibank.domain.LedgerEntry;
import se.ivank.minibank.domain.Transfer;
import se.ivank.minibank.dto.TransferRequest;
import se.ivank.minibank.exception.AccountNotFoundException;
import se.ivank.minibank.exception.InvalidOperationException;
import se.ivank.minibank.repository.AccountRepository;
import se.ivank.minibank.repository.LedgerEntryRepository;
import se.ivank.minibank.repository.TransferRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BankService {

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    private final TransferRepository transfers;

    public BankService(AccountRepository accounts, LedgerEntryRepository ledger,
                       TransferRepository transfers) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.transfers = transfers;
    }

    public Account createAccount(String ownerName) {
        return accounts.save(new Account(ownerName));
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    public Account deposit(Long id, BigDecimal amount) {
        Account account = getAccount(id);
        account.deposit(amount);
        ledger.save(new LedgerEntry(id, EntryType.DEPOSIT, amount, null));
        return account;
    }

    public Account withdraw(Long id, BigDecimal amount) {
        Account account = getAccount(id);
        account.withdraw(amount);
        ledger.save(new LedgerEntry(id, EntryType.WITHDRAWAL, amount, null));
        return account;
    }

    /**
     * Moves money atomically: either both accounts change and both ledger entries are written,
     * or nothing happens. The same Idempotency-Key never moves money twice.
     */
    public TransferResult transfer(String idempotencyKey, TransferRequest request) {
        Optional<Transfer> existing = transfers.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new TransferResult(existing.get(), true);
        }
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new InvalidOperationException("Cannot transfer to the same account");
        }

        Account from = getAccount(request.fromAccountId());
        Account to = getAccount(request.toAccountId());
        BigDecimal amount = request.amount();

        from.withdraw(amount);
        to.deposit(amount);

        ledger.save(new LedgerEntry(from.getId(), EntryType.TRANSFER_OUT, amount, to.getId()));
        ledger.save(new LedgerEntry(to.getId(), EntryType.TRANSFER_IN, amount, from.getId()));

        Transfer saved = transfers.saveAndFlush(
                new Transfer(idempotencyKey, from.getId(), to.getId(), amount));
        return new TransferResult(saved, false);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> history(Long accountId) {
        getAccount(accountId);
        return ledger.findByAccountIdOrderByIdDesc(accountId);
    }
}
