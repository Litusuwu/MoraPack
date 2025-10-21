package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {
  List<Alert> findByStatus(String status);
  List<Alert> findByOrder_Id(Integer orderId);
  List<Alert> findByDescriptionContainingIgnoreCase(String description);
  List<Alert> findByIdIn(List<Integer> ids);
  boolean existsByDescriptionAndOrder_Id(String description, Integer orderId);
  @Modifying
  @Query("DELETE FROM Alert a WHERE a.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
