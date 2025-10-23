package com.system.morapack.schemas;

import lombok.*;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class CitySchema {
  private Integer id;
  private String name;
  private Continent continent;
}