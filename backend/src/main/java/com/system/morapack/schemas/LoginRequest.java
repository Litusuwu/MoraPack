package com.system.morapack.schemas;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String username; // Could be email or username
    private String password;
}
