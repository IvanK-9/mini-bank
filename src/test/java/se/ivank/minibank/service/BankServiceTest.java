package se.ivank.minibank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.ivank.minibank.domain.Account;
import se.ivank.minibank.domain.LedgerEntry;
import se.ivank.minibank.domain.Transfer;
import se.ivank.minibank.dto.TransferRequest;
import se.ivank.minibank.exception.AccountNotFoundException;
import se.ivank.minibank.exception.InsufficientFundsException;
import se.ivank.minibank.exception.InvalidOperationException;
import se.ivank.minibank.repository.AccountRepository;
import se.ivank.minibank.repository.LedgerEntryRepository;
import se.ivank.minibank.repository.TransferRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    AccountRepository accounts;
    @Mock
    LedgerEntryRepository ledger;
    @Mock
    TransferRepository transfers;

    @InjectMocks
    BankService service;

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static Account account(long id, String balance) {
        Account a = new Account("owner-" + id);
        ReflectionTestUtils.setField(a, "id", id);
        if (money(balance).signum() > 0) {
            a.deposit(money(balance));
        }
        return a;
    }

    @Test
    void transferMovesMoneyAndWritesTwoLedgerEntries() {
        Account from = account(1L, "100.00");
        Account to = account(2L, "5.00");
        when(transfers.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        when(accounts.findById(1L)).thenReturn(Optional.of(from));
        when(accounts.findById(2L)).thenReturn(Optional.of(to));
        when(transfers.saveAndFlush(any(Transfer.class))).thenAnswer(i -> i.getArgument(0));

        TransferResult result = service.transfer("k1",
                new TransferRequest(1L, 2L, money("30.00")));

        assertFalse(result.replayed());
        assertEquals(0, money("70.00").compareTo(from.getBalance()));
        assertEquals(0, money("35.00").compareTo(to.getBalance()));
        verify(ledger, times(2)).save(any(LedgerEntry.class));
    }

    @Test
    void transferWithInsufficientFundsChangesNothing() {
        Account from = account(1L, "10.00");
        Account to = account(2L, "0.00");
        when(transfers.findByIdempotencyKey("k2")).thenReturn(Optional.empty());
        when(accounts.findById(1L)).thenReturn(Optional.of(from));
        when(accounts.findById(2L)).thenReturn(Optional.of(to));

        assertThrows(InsufficientFundsException.class, () ->
                service.transfer("k2", new TransferRequest(1L, 2L, money("50.00"))));

        assertEquals(0, money("10.00").compareTo(from.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(to.getBalance()));
        verify(ledger, never()).save(any());
        verify(transfers, never()).saveAndFlush(any());
    }

    @Test
    void transferToSameAccountIsRejected() {
        when(transfers.findByIdempotencyKey("k3")).thenReturn(Optional.empty());
        assertThrows(InvalidOperationException.class, () ->
                service.transfer("k3", new TransferRequest(1L, 1L, money("1.00"))));
        verifyNoInteractions(accounts, ledger);
    }

    @Test
    void sameIdempotencyKeyDoesNotMoveMoneyTwice() {
        Transfer already = new Transfer("k4", 1L, 2L, money("30.00"));
        when(transfers.findByIdempotencyKey("k4")).thenReturn(Optional.of(already));

        TransferResult result = service.transfer("k4",
                new TransferRequest(1L, 2L, money("30.00")));

        assertTrue(result.replayed());
        assertSame(already, result.transfer());
        verifyNoInteractions(accounts, ledger);
    }

    @Test
    void unknownAccountThrowsNotFound() {
        when(accounts.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> service.getAccount(99L));
    }

    @Test
    void depositUpdatesBalanceAndWritesLedgerEntry() {
        Account acc = account(1L, "0.00");
        when(accounts.findById(1L)).thenReturn(Optional.of(acc));

        service.deposit(1L, money("25.00"));

        assertEquals(0, money("25.00").compareTo(acc.getBalance()));
        verify(ledger).save(any(LedgerEntry.class));
    }
}
