package com.jewelry.service;

import com.jewelry.entity.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Business logic contract for Customer management.
 */
public interface CustomerService {

    Customer createCustomer(Customer customer);

    Optional<Customer> findById(Long id);

    List<Customer> findAll();

    void updateCustomer(Customer customer);

    void deleteCustomer(Long id);

    List<Customer> search(String keyword);

    Optional<Customer> findByEmail(String email);
}
