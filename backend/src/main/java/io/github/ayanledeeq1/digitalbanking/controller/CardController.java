package io.github.ayanledeeq1.digitalbanking.controller;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.github.ayanledeeq1.digitalbanking.dto.cardDto.CardResponseDto;
import io.github.ayanledeeq1.digitalbanking.service.CardService;

@RestController
@RequestMapping("/api/customers")
public class CardController {
    private final CardService cardService;
    public CardController(CardService cardService) { this.cardService = cardService; }
    @GetMapping("/card")
    public ResponseEntity<CardResponseDto> getCard(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(cardService.getCurrentCard(authentication.getName()));
    }

    @org.springframework.web.bind.annotation.PostMapping("/card/pin")
    public ResponseEntity<io.github.ayanledeeq1.digitalbanking.dto.cardDto.CardPinResponseDto> revealPin(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody
            io.github.ayanledeeq1.digitalbanking.dto.cardDto.CardPinRevealRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(cardService.revealPin(authentication.getName(), request.password()));
    }
}
