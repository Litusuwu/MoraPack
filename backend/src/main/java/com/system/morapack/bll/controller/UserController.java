package com.system.morapack.bll.controller;

import com.system.morapack.bll.adapter.UserAdapter;
import com.system.morapack.schemas.UserSchema;
import com.system.morapack.schemas.TypeUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserController {

  private final UserAdapter userAdapter;

  public UserSchema getUser(Integer id) {
    return userAdapter.getUser(id);
  }

  public List<UserSchema> fetchUsers(List<Integer> ids) {
    return userAdapter.fetchUsers(ids);
  }

  public UserSchema createUser(UserSchema request) {
    return userAdapter.createUser(request);
  }

  public List<UserSchema> bulkCreateUsers(List<UserSchema> requests) {
    return userAdapter.bulkCreateUsers(requests);
  }

  public UserSchema updateUser(Integer id, UserSchema request) {
    return userAdapter.updateUser(id, request);
  }

  public void deleteUser(Integer id) {
    userAdapter.deleteUser(id);
  }

  public void bulkDeleteUsers(List<Integer> ids) {
    userAdapter.bulkDeleteUsers(ids);
  }

  public List<UserSchema> getUsersByType(TypeUser type) {
    return userAdapter.getUsersByType(type);
  }
}
