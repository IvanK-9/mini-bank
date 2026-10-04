package se.ivank.minibank.domain;

import org.junit.jupiter.api.Test;
import se.ivank.minibank.exception.InsufficientFundsException;
import se.ivank.minibank.exception.InvalidOperationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    @Test
    void depositIncreasesBalance() {
        Account account = new Account("Ivan");
        account.deposit(money("100.50"));
        assertEquals(0, money("100.50").compareTo(account.getBalance()));
    }

    @Test
    void withdrawDecreasesBalance() {
        Account account = new Account("Ivan");
        account.deposit(money("100.00"));
        account.withdraw(money("40.25"));
        assertEquals(0, money("59.75").compareTo(account.getBalance()));
    }

    @Test
    void withdrawMoreThanBalanceThrowsAndKeepsBalance() {
        Account account = new Account("Ivan");
        account.deposit(money("10.00"));
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(money("10.01")));
        assertEquals(0, money("10.00").compareTo(account.getBalance()));
    }

    @Test
    void withdrawingExactBalanceIsAllowed() {
        Account account = new Account("Ivan");
        account.deposit(money("10.00"));
        account.withdraw(money("10.00"));
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
    }

    @Test
    void rejectsZeroAndNegativeAmounts() {
        Account account = new Account("Ivan");
        assertThrows(InvalidOperationException.class, () -> account.deposit(BigDecimal.ZERO));
        assertThrows(InvalidOperationException.class, () -> account.deposit(money("-5")));
        assertThrows(InvalidOperationException.class, () -> account.withdraw(money("-5")));
    }
}
