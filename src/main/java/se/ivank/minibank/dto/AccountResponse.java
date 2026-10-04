package se.ivank.minibank.dto;

import se.ivank.minibank.domain.Account;

import java.math.BigDecimal;

public record AccountResponse(Long id, String ownerName, BigDecimal balance) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getOwnerName(), a.getBalance());
    }
}
