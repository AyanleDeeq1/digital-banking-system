package io.github.ayanledeeq1.digitalbanking.dto.cardDto;
import jakarta.validation.constraints.NotBlank;

public record CardPinRevealRequestDto(@NotBlank(message = "Enter your application password") String password) {
    @Override public String toString() { return "CardPinRevealRequestDto[REDACTED]"; }
}
