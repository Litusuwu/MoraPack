package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSchema {
  private Integer id;
  private String name;
  private String lastName;
  private TypeUser type;
  private LocalDateTime creationDate;
  private LocalDateTime updatedDate;
}