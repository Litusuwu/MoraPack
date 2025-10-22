package com.system.morapack.schemas;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String name;
    private String lastName;
    private String username;
    private String password;
    private TypeUser type;
}
