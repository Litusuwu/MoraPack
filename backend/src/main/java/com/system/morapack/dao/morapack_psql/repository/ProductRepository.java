package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
  List<Product> findByNameContainingIgnoreCase(String name);
  List<Product> findByOrder_Id(Integer orderId);
  List<Product> findByIdIn(List<Integer> ids);
  boolean existsByName(String name);
  @Modifying
  @Query("DELETE FROM Product p WHERE p.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}