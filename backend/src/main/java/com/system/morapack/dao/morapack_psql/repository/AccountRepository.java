package com.system.morapack.dao.morapack_psql.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.system.morapack.dao.morapack_psql.model.Account;
import java.util.List;


@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
  List<Account> findByIdAccountIn(List<Integer> id);
}
