package com.system.morapack.schemas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Airport {
  private int id;
  private String codeIATA;
  private String alias;
  private int timezoneUTC;
  private String latitude;
  private String longitude;
  private City city;
  private AirportState state;
  private Warehouse warehouse;
}
