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
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private  Customer customer;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    protected Card() {}

    public Card(String cardNumber, String cvc2, LocalDate expiryDate, Customer customer, Account account) {
        if (cardNumber == null || !cardNumber.matches("[0-9]{16}")) {
            throw new IllegalArgumentException("Card number must contain exactly 16 digits");
        }
        if (cvc2 == null || !cvc2.matches("[0-9]{3}")) {
            throw new IllegalArgumentException("CVC2 must contain exactly 3 digits");
        }
        this.customer = Objects.requireNonNull(customer, "Customer is required");
        this.account = Objects.requireNonNull(account, "Account is required");
        if (customer.getId() == null || account.getCustomer() == null
                || !customer.getId().equals(account.getCustomer().getId())) {
            throw new IllegalArgumentException("Card account must belong to its customer");
        }
        this.cardNumber = cardNumber;
        this.cvc2 = cvc2;
        this.expiryDate = Objects.requireNonNull(expiryDate, "Expiry date is required");
    }

    public Long getId() { return id; }
    public String getCardNumber() { return cardNumber; }
    public String getCvc2() { return cvc2; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public Customer getCustomer() { return customer; }
    public Account getAccount() { return account; }


}
