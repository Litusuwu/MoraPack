package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.Customer;
import com.system.morapack.dao.morapack_psql.model.User;
import com.system.morapack.dao.morapack_psql.service.CustomerService;
import com.system.morapack.dao.morapack_psql.service.UserService;
import com.system.morapack.schemas.CustomerSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomerAdapter {

  private final CustomerService customerService;
  private final UserService userService;

  private CustomerSchema mapToSchema(Customer customer) {
    return CustomerSchema.builder()
        .id(customer.getId())
        .phone(customer.getPhone())
        .fiscalAddress(customer.getFiscalAddress())
        .createdAt(customer.getCreatedAt())
        .personId(customer.getPerson() != null ? customer.getPerson().getId() : null)
        .personName(customer.getPerson() != null ? customer.getPerson().getName() : null)
        .personLastName(customer.getPerson() != null ? customer.getPerson().getLastName() : null)
        .build();
  }

  private Customer mapToEntity(CustomerSchema schema) {
    Customer.CustomerBuilder builder = Customer.builder()
        .id(schema.getId())
        .phone(schema.getPhone())
        .fiscalAddress(schema.getFiscalAddress())
        .createdAt(schema.getCreatedAt());

    if (schema.getPersonId() != null) {
      User person = userService.getUser(schema.getPersonId());
      builder.person(person);
    }

    return builder.build();
  }

  public CustomerSchema getCustomer(Integer id) {
    Customer customer = customerService.getCustomer(id);
    return mapToSchema(customer);
  }

  public List<CustomerSchema> fetchCustomers(List<Integer> ids) {
    return customerService.fetchCustomers(ids).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public CustomerSchema createCustomer(CustomerSchema schema) {
    Customer entity = mapToEntity(schema);
    return mapToSchema(customerService.createCustomer(entity));
  }

  public List<CustomerSchema> bulkCreateCustomers(List<CustomerSchema> schemas) {
    List<Customer> entities = schemas.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
    return customerService.bulkCreateCustomers(entities).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public CustomerSchema updateCustomer(Integer id, CustomerSchema updates) {
    Customer entityUpdates = mapToEntity(updates);
    return mapToSchema(customerService.updateCustomer(id, entityUpdates));
  }

  public void deleteCustomer(Integer id) {
    customerService.deleteCustomer(id);
  }

  public void bulkDeleteCustomers(List<Integer> ids) {
    customerService.bulkDeleteCustomers(ids);
  }
}
