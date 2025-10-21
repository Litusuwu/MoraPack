package com.system.morapack.dao.morapack_psql.service;

import com.system.morapack.dao.morapack_psql.model.User;
import com.system.morapack.dao.morapack_psql.repository.UserRepository;
import com.system.morapack.schemas.TypeUser;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public User getUser(Integer id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
  }

  public List<User> fetchUsers(List<Integer> ids) {
    if (ids == null || ids.isEmpty()) {
      return userRepository.findAll();
    }
    return userRepository.findByIdIn(ids);
  }

  public User createUser(User user) {
    if (userRepository.existsByNameAndLastName(user.getName(), user.getLastName())) {
      throw new IllegalArgumentException("User already exists: " + user.getName() + " " + user.getLastName());
    }
    return userRepository.save(user);
  }

  public List<User> bulkCreateUsers(List<User> users) {
    return userRepository.saveAll(users);
  }

  public User updateUser(Integer id, User updates) {
    User user = getUser(id);

    if (updates.getName() != null)
      user.setName(updates.getName());
    if (updates.getLastName() != null)
      user.setLastName(updates.getLastName());
    if (updates.getUserType() != null)
      user.setUserType(updates.getUserType());

    return userRepository.save(user);
  }

  public void deleteUser(Integer id) {
    if (!userRepository.existsById(id)) {
      throw new EntityNotFoundException("User not found with id: " + id);
    }
    userRepository.deleteById(id);
  }

  @Transactional
  public void bulkDeleteUsers(List<Integer> ids) {
    userRepository.deleteAllByIdIn(ids);
  }

  // Buscar usuarios por tipo (ejemplo: ADMIN, CLIENT, etc.)
  public List<User> getUsersByType(TypeUser type) {
    return userRepository.findByUserType(type);
  }
}
