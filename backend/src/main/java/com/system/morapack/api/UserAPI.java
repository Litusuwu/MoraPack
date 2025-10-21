package com.system.morapack.api;

import com.system.morapack.bll.controller.UserController;
import com.system.morapack.schemas.UserSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAPI {

  private final UserController userController;

  @GetMapping("/{id}")
  public ResponseEntity<UserSchema> getUser(@PathVariable Integer id) {
    return ResponseEntity.ok(userController.getUser(id));
  }

  @GetMapping
  public ResponseEntity<List<UserSchema>> getUsers(
      @RequestParam(required = false) List<Integer> ids) {
    return ResponseEntity.ok(userController.fetchUsers(ids));
  }

  @PostMapping
  public ResponseEntity<UserSchema> createUser(@RequestBody UserSchema user) {
    return ResponseEntity.ok(userController.createUser(user));
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserSchema> updateUser(@PathVariable Integer id, @RequestBody UserSchema updates) {
    return ResponseEntity.ok(userController.updateUser(id, updates));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
    userController.deleteUser(id);
    return ResponseEntity.noContent().build();
  }
}