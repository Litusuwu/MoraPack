package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.CustomerAdapter;
import com.system.morapack.schemas.CustomerSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerAdapter customerAdapter;

  public CustomerSchema getCustomer(Integer id) {
    return customerAdapter.getCustomer(id);
  }

  public List<CustomerSchema> fetchCustomers(List<Integer> ids) {
    return customerAdapter.fetchCustomers(ids);
  }

  public CustomerSchema createCustomer(CustomerSchema request) {
    return customerAdapter.createCustomer(request);
  }

  public List<CustomerSchema> bulkCreateCustomers(List<CustomerSchema> requests) {
    return customerAdapter.bulkCreateCustomers(requests);
  }

  public CustomerSchema updateCustomer(Integer id, CustomerSchema request) {
    return customerAdapter.updateCustomer(id, request);
  }

  public void deleteCustomer(Integer id) {
    customerAdapter.deleteCustomer(id);
  }

  public void bulkDeleteCustomers(List<Integer> ids) {
    customerAdapter.bulkDeleteCustomers(ids);
  }
}
