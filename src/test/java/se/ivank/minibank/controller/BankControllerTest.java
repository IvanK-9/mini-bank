package se.ivank.minibank.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import se.ivank.minibank.domain.Transfer;
import se.ivank.minibank.exception.AccountNotFoundException;
import se.ivank.minibank.exception.InsufficientFundsException;
import se.ivank.minibank.service.BankService;
import se.ivank.minibank.service.TransferResult;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankController.class)
class BankControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    BankService service;

    @Test
    void getUnknownAccountReturns404() throws Exception {
        when(service.getAccount(7L)).thenThrow(new AccountNotFoundException(7L));
        mvc.perform(get("/api/accounts/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account 7 not found"));
    }

    @Test
    void withdrawTooMuchReturns422() throws Exception {
        when(service.withdraw(eq(1L), any())).thenThrow(
                new InsufficientFundsException(1L, new BigDecimal("5.00"), new BigDecimal("50.00")));
        mvc.perform(post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 50.00}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void depositRejectsNegativeAmount() throws Exception {
        mvc.perform(post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void depositRejectsMoreThanTwoDecimals() throws Exception {
        mvc.perform(post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1.234}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferWithoutIdempotencyKeyReturns400() throws Exception {
        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":10.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void newTransferReturns201() throws Exception {
        Transfer t = new Transfer("key-1", 1L, 2L, new BigDecimal("10.00"));
        when(service.transfer(eq("key-1"), any())).thenReturn(new TransferResult(t, false));
        mvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":10.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromAccountId").value(1))
                .andExpect(jsonPath("$.toAccountId").value(2));
    }

    @Test
    void replayedTransferReturns200() throws Exception {
        Transfer t = new Transfer("key-1", 1L, 2L, new BigDecimal("10.00"));
        when(service.transfer(eq("key-1"), any())).thenReturn(new TransferResult(t, true));
        mvc.perform(post("/api/transfers")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":10.00}"))
                .andExpect(status().isOk());
    }
}
