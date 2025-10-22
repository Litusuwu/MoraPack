package com.system.morapack.schemas;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AirportSchema {
  private Integer id;
  private String codeIATA;
  private String alias;
  private Integer timezoneUTC;
  private String latitude;
  private String longitude;
  private Integer cityId;
  private String cityName;
  private AirportState state;
  private Integer warehouseId;

  // Legacy fields for algorithm compatibility
  private CitySchema citySchema;
  private Warehouse warehouse;
}
