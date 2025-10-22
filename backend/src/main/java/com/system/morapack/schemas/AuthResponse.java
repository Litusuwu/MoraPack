package com.system.morapack.schemas;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Boolean success;
    private String message;
    private SessionSchema session;
}
