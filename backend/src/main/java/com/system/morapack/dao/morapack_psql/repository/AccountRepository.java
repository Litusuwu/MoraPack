package com.system.morapack.dao.morapack_psql.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.system.morapack.dao.morapack_psql.model.Account;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
  List<Account> findByIdIn(List<Integer> ids);
  Optional<Account> findByEmail(String email);
  boolean existsByEmail(String email);
}
