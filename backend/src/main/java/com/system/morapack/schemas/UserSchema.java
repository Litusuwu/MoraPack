package com.system.morapack.schemas;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSchema {
  private int id;
  private String name;
  private String lastName;
  private TypeUser userType;
  private String creationDate;
  private String updatedDate;
  private List<AccountSchema> accountSchemas;
}
