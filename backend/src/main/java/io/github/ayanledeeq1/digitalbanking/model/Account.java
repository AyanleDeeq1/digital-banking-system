package io.github.ayanledeeq1.digitalbanking.model;

import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity 
public class Account {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;
    @Column(nullable = false, unique = true)
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private  Customer customer;

    protected Account() {}

    public Account(String accountNumber, AccountStatus status) {
        this.accountNumber = accountNumber;
        this.status= status;
    }


    public  String getAccountNumber() {
        return  accountNumber;
    }

    public AccountStatus getStatus() {
        return  status;
    }

    void  setCustomer(Customer customer) {
        this.customer = customer;
    }

}
