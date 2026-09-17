package io.github.ayanledeeq1.digitalbanking.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity 
public class Customer {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;
    @Column(nullable = false)
    private  String firstName;
    @Column(nullable = false)
    private  String lastName;
    @Column(nullable = false, unique = true)
    private  String email;
    @OneToMany(mappedBy = "customer")
    private List<Account> accounts = new ArrayList<>();
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "password_credential_id", nullable = false, unique = true)
    private PasswordCredential passwordCredential;
    // requiered by JPA to instantiate the entity.
    protected  Customer(){} 

    public  Customer(String firstName, String lastName, String email, PasswordCredential passwordCredential) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordCredential = passwordCredential;
    }


    public String getFirstName() {
        return  firstName;
    }

    public  String getLastNAme() {
        return  lastName;
    }

    public String getEmail(){
        return  email;
    }

    public List<Account> getAccounts() {
    return accounts;
    }
    
    public void  addAccount(Account account) {
        accounts.add(account);
        account.setCustomer(this);
    }

    public  PasswordCredential getPasswordCredential() {
        return passwordCredential;
    }
}
