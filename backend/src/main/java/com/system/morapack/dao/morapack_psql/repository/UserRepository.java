package com.system.morapack.dao.morapack_psql.repository;

import com.system.morapack.dao.morapack_psql.model.User;
import com.system.morapack.schemas.TypeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
  Optional<User> findByNameAndLastName(String name, String lastName);
  List<User> findByType(TypeUser type);
  List<User> findByIdIn(List<Integer> ids);
  boolean existsByNameAndLastName(String name, String lastName);
  @Modifying
  @Query("DELETE FROM User u WHERE u.id IN :ids")
  void deleteAllByIdIn(List<Integer> ids);
}
