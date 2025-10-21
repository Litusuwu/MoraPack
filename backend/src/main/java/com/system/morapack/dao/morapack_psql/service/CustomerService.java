package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.Customer;
import com.system.morapack.dao.morapack_psql.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CustomerRepository customerRepository;

  public Customer getCustomer(Integer id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));
  }

  public List<Customer> fetchCustomers(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) {
      return customerRepository.findAll();
    }
    return customerRepository.findByIdIn(ids);
  }

  public Customer createCustomer(Customer customer) {
    if (customerRepository.existsByPhone(customer.getPhone())) {
      throw new IllegalArgumentException("Phone already exists: " + customer.getPhone());
    }
    return customerRepository.save(customer);
  }

  public List<Customer> bulkCreateCustomers(List<Customer> customers) {
    return customerRepository.saveAll(customers);
  }

  public Customer updateCustomer(Integer id, Customer updates) {
    Customer customer = getCustomer(id);
    if (updates.getPhone() != null)
      customer.setPhone(updates.getPhone());
    if (updates.getFiscalAddress() != null)
      customer.setFiscalAddress(updates.getFiscalAddress());
    return customerRepository.save(customer);
  }

  public void deleteCustomer(Integer id) {
    if (!customerRepository.existsById(id)) {
      throw new EntityNotFoundException("Customer not found with id: " + id);
    }
    customerRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteCustomers(List<Integer> ids) {
    customerRepository.deleteAllByIdIn(ids);
  }
}
