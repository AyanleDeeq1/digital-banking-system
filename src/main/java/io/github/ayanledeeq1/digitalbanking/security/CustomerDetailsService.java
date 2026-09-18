package io.github.ayanledeeq1.digitalbanking.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import io.github.ayanledeeq1.digitalbanking.exception.CustomerNotFoundException;
import io.github.ayanledeeq1.digitalbanking.model.Customer;
import io.github.ayanledeeq1.digitalbanking.repository.CustomerRepository;
@Service 
public class CustomerDetailsService  implements UserDetailsService{
    private final CustomerRepository customerRepository;

    public CustomerDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new CustomerNotFoundException("customer with email " + email + " not found" ));
        return  org.springframework.security.core.userdetails.User
                  .withUsername(customer.getEmail())
                  .password(customer.getPasswordCredential().getHashedPassword())
                  .build();
    
    }
    
}
