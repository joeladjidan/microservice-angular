package com.joeladjidan.anstic.customer.service;

import com.joeladjidan.anstic.customer.domain.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerService {
    List<Customer> findAll();
    Optional<Customer> findById(Long id);
    Customer save(Customer customer);
    void deleteById(Long id);
}

