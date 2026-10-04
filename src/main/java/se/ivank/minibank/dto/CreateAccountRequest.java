package se.ivank.minibank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(@NotBlank @Size(max = 100) String ownerName) {
}
