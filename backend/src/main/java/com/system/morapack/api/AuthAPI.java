package com.system.morapack.api;

import com.system.morapack.bll.controller.AuthController;
import com.system.morapack.schemas.AuthResponse;
import com.system.morapack.schemas.LoginRequest;
import com.system.morapack.schemas.RegisterRequest;
import com.system.morapack.schemas.SessionSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthAPI {

  private final AuthController authController;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
    AuthResponse response = authController.register(request);
    if (response.getSuccess()) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.badRequest().body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    AuthResponse response = authController.login(request);
    if (response.getSuccess()) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.badRequest().body(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<AuthResponse> logout(@RequestParam Integer sessionId) {
    AuthResponse response = authController.logout(sessionId);
    if (response.getSuccess()) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.badRequest().body(response);
  }

  @GetMapping("/session/{sessionId}")
  public ResponseEntity<SessionSchema> getSession(@PathVariable Integer sessionId) {
    SessionSchema session = authController.getSession(sessionId);
    if (session != null) {
      return ResponseEntity.ok(session);
    }
    return ResponseEntity.notFound().build();
  }
}
