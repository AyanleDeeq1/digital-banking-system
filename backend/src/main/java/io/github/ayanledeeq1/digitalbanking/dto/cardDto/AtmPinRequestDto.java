package io.github.ayanledeeq1.digitalbanking.dto.cardDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AtmPinRequestDto(
        @NotNull @Pattern(regexp = "[0-9]{4}", message = "Enter a four-digit PIN") String pin) {
    @Override public String toString() { return "AtmPinRequestDto[REDACTED]"; }
}
