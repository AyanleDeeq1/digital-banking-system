package io.github.ayanledeeq1.digitalbanking.model;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;


@Entity
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;
    @Column(nullable = false, unique = true, length = 16)
    private String cardNumber;
    @Column(nullable = false, length = 3)
    private String cvc2;
    @Column(nullable = false)
    private LocalDate expiryDate;
    @Column(name = "pin", nullable = false)
    private String encryptedPin;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    protected Card() {}

    public Card(String cardNumber, String cvc2, LocalDate expiryDate, String encryptedPin, Account account) {
        if (cardNumber == null || !cardNumber.matches("[0-9]{16}")) {
            throw new IllegalArgumentException("Card number must contain exactly 16 digits");
        }
        if (cvc2 == null || !cvc2.matches("[0-9]{3}")) {
            throw new IllegalArgumentException("CVC2 must contain exactly 3 digits");
        }
        if (encryptedPin == null || !encryptedPin.matches("v1:[A-Za-z0-9+/]{43}=")) {
            throw new IllegalArgumentException("An encrypted PIN is required");
        }
        this.account = Objects.requireNonNull(account, "Account is required");
        if (account.getCustomer() == null) {
            throw new IllegalArgumentException("Card account must belong to a customer");
        }
        this.cardNumber = cardNumber;
        this.cvc2 = cvc2;
        this.expiryDate = Objects.requireNonNull(expiryDate, "Expiry date is required");
        this.encryptedPin = encryptedPin;
    }

    public Long getId() { return id; }
    public String getCardNumber() { return cardNumber; }
    public String getCvc2() { return cvc2; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public Account getAccount() { return account; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getEncryptedPin() { return encryptedPin; }


}
