package io.github.ayanledeeq1.digitalbanking.model;

import io.github.ayanledeeq1.digitalbanking.enums.AccountStatus;
import io.github.ayanledeeq1.digitalbanking.enums.AccountType;
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
    @Column(nullable = false)
    private  String name;
    @Column(nullable = false, unique = true)
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private  Customer customer;

    protected Account() {}

    public Account(String name, String accountNumber, AccountType accountType, AccountStatus status) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.status= status;

    }


    public  String getAccountNumber() {
        return  accountNumber;
    }

    public AccountStatus getStatus() {
        return  status;
    }

    public  AccountType getType() {
        return  accountType;
    }

    public  String getAccountName() {
        return  name;
    }

    void  setCustomer(Customer customer) {
        this.customer = customer;
    }

    public  Long getId() {
        return  id;
    }

}
