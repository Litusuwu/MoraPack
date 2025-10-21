package com.system.morapack.bll.adapter;

import com.system.morapack.dao.morapack_psql.model.User;
import com.system.morapack.dao.morapack_psql.service.UserService;
import com.system.morapack.schemas.UserSchema;
import com.system.morapack.schemas.TypeUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserAdapter {

  private final UserService userService;

  private UserSchema mapToSchema(User user) {
    return UserSchema.builder()
        .id(user.getId())
        .name(user.getName())
        .lastName(user.getLastName())
        .type(user.getUserType())
        .creationDate(user.getCreationDate())
        .updatedDate(user.getUpdatedDate())
        .build();
  }

  private User mapToEntity(UserSchema schema) {
    return User.builder()
        .id(schema.getId())
        .name(schema.getName())
        .lastName(schema.getLastName())
        .userType(schema.getType())
        .build();
  }

  public UserSchema getUser(Integer id) {
    User user = userService.getUser(id);
    return mapToSchema(user);
  }

  public List<UserSchema> fetchUsers(List<Integer> ids) {
    return userService.fetchUsers(ids).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public UserSchema createUser(UserSchema schema) {
    User entity = mapToEntity(schema);
    return mapToSchema(userService.createUser(entity));
  }

  public List<UserSchema> bulkCreateUsers(List<UserSchema> schemas) {
    List<User> entities = schemas.stream()
        .map(this::mapToEntity)
        .collect(Collectors.toList());
    return userService.bulkCreateUsers(entities).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }

  public UserSchema updateUser(Integer id, UserSchema updates) {
    User entityUpdates = mapToEntity(updates);
    return mapToSchema(userService.updateUser(id, entityUpdates));
  }

  public void deleteUser(Integer id) {
    userService.deleteUser(id);
  }

  public void bulkDeleteUsers(List<Integer> ids) {
    userService.bulkDeleteUsers(ids);
  }

  public List<UserSchema> getUsersByType(TypeUser type) {
    return userService.getUsersByType(type).stream()
        .map(this::mapToSchema)
        .collect(Collectors.toList());
  }
}