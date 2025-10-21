package com.system.morapack.dao.morapack_psql.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.system.morapack.dao.morapack_psql.model.Customer;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer>{
  Optional<Customer> findByPhone(String phone);

  Optional<Customer> findByFiscalAddress(String fiscalAddress);

  List<Customer> findByPerson_Id(Integer personId);

  List<Customer> findByIdIn(List<Integer> ids);

  boolean existsByPhone(String phone);

  @Modifying
  @Query("DELETE FROM Customer c WHERE c.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
