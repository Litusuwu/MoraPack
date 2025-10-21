package com.system.morapack.dao.morapack_psql.controller;

import java.util.List;

import org.springframework.stereotype.Service;
import com.system.morapack.dao.morapack_psql.repository.AccountRepository;
import com.system.morapack.dao.morapack_psql.model.Account;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountController {
  private final AccountRepository accountRepository;

  public Account getAccount(Integer id) {
    return accountRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + id));
  }

  public List<Account> fetchAccounts(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) {
      return accountRepository.findAll();
    }
    return accountRepository.findByIdAccountIn(ids);
  }

  public Account createAccount(Account account) {
    return accountRepository.save(account);
  }

  public List<Account> bulkCreateAccounts(List<Account> accounts) {
    return accountRepository.saveAll(accounts);
  }

  public Account updateAccount(Integer id, Account updates) {
    Account account = getAccount(id);

    if (updates.getEmail() != null) {
      account.setEmail(updates.getEmail());
    }
    if (updates.getPassword() != null) {
      account.setPassword(updates.getPassword());
    }

    return accountRepository.save(account);
  }

  public void deleteAccount(Integer id) {
    if (!accountRepository.existsById(id)) {
      throw new EntityNotFoundException("Account not found with id: " + id);
    }
    accountRepository.deleteById(id);
  }
 
  public void bulkDeleteAccounts(List<Integer> ids) {
    accountRepository.deleteAllById(ids);
  }

}
