package se.ivank.minibank.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import se.ivank.minibank.domain.Account;
import se.ivank.minibank.dto.*;
import se.ivank.minibank.exception.InvalidOperationException;
import se.ivank.minibank.service.BankService;
import se.ivank.minibank.service.TransferResult;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BankController {

    private final BankService service;

    public BankController(BankService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = service.createAccount(request.ownerName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(account.getId()).toUri();
        return ResponseEntity.created(location).body(AccountResponse.from(account));
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return AccountResponse.from(service.getAccount(id));
    }

    @PostMapping("/accounts/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(service.deposit(id, request.amount()));
    }

    @PostMapping("/accounts/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(service.withdraw(id, request.amount()));
    }

    @GetMapping("/accounts/{id}/transactions")
    public List<EntryResponse> history(@PathVariable Long id) {
        return service.history(id).stream().map(EntryResponse::from).toList();
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 64) {
            throw new InvalidOperationException("Idempotency-Key must be 1-64 characters");
        }
        TransferResult result = service.transfer(idempotencyKey, request);
        TransferResponse body = TransferResponse.from(result.transfer());
        return result.replayed()
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(201).body(body);
    }
}
