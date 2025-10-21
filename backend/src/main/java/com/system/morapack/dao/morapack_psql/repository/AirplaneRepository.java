package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.Airplane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AirplaneRepository extends JpaRepository<Airplane, Integer> {

  List<Airplane> findByIdIn(List<Integer> ids);

  Optional<Airplane> findByRegistration(String registration);
  boolean existsByRegistration(String registration);

  List<Airplane> findByStatus(String status);
  List<Airplane> findByCapacityGreaterThanEqual(Integer minCapacity);
  List<Airplane> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

  @Modifying
  @Query("DELETE FROM Airplane a WHERE a.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
