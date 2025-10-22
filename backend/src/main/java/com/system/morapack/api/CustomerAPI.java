package com.system.morapack.api;

import com.system.morapack.bll.controller.CustomerController;
import com.system.morapack.schemas.CustomerSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerAPI {

  private final CustomerController customerController;

  @GetMapping("/{id}")
  public ResponseEntity<CustomerSchema> getCustomer(@PathVariable Integer id) {
    return ResponseEntity.ok(customerController.getCustomer(id));
  }

  @GetMapping
  public ResponseEntity<List<CustomerSchema>> getCustomers(
      @RequestParam(required = false) List<Integer> ids) {
    return ResponseEntity.ok(customerController.fetchCustomers(ids));
  }

  @PostMapping
  public ResponseEntity<CustomerSchema> createCustomer(@RequestBody CustomerSchema customer) {
    return ResponseEntity.ok(customerController.createCustomer(customer));
  }

  @PutMapping("/{id}")
  public ResponseEntity<CustomerSchema> updateCustomer(@PathVariable Integer id, @RequestBody CustomerSchema updates) {
    return ResponseEntity.ok(customerController.updateCustomer(id, updates));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
    customerController.deleteCustomer(id);
    return ResponseEntity.noContent().build();
  }
}
