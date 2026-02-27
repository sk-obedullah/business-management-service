package com.jewelry.service.impl;

import com.jewelry.entity.Customer;
import com.jewelry.exception.DuplicateEntityException;
import com.jewelry.exception.EntityNotFoundException;
import com.jewelry.repository.CustomerRepository;
import com.jewelry.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Production implementation of {@link CustomerService}.
 */
@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public Customer createCustomer(Customer customer) {
        if (customer == null)
            throw new IllegalArgumentException("Customer must not be null");
        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            throw new IllegalArgumentException("Customer phone number must not be blank");
        }
        if (customerRepository.existsByPhone(customer.getPhone())) {
            throw new DuplicateEntityException(
                    "Customer with phone '" + customer.getPhone() + "' already exists.");
        }
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            if (customerRepository.existsByEmail(customer.getEmail())) {
                throw new DuplicateEntityException(
                        "Customer with email '" + customer.getEmail() + "' already exists.");
            }
        }
        Customer saved = customerRepository.save(customer);
        log.info("Created customer id={}", saved.getId());
        return saved;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public void updateCustomer(Customer customer) {
        if (customer == null || customer.getId() == null)
            throw new IllegalArgumentException("Customer and ID must not be null");
        customerRepository.findById(customer.getId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", customer.getId()));
        customerRepository.save(customer);
        log.info("Updated customer id={}", customer.getId());
    }

    @Override
    public void deleteCustomer(Long id) {
        customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer", id));
        customerRepository.deleteById(id);
        log.info("Deleted customer id={}", id);
    }

    @Override
    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.isBlank())
            return findAll();
        return customerRepository.search(keyword.trim());
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }
}
