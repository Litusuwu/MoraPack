package com.system.morapack.dao.morapack_psql.model;

import com.system.morapack.schemas.AirportState;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "airports")
public class Airport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @Column(name = "code_iata", nullable = false, length = 8)
  private String codeIATA;

  @Column(name = "alias", length = 120)
  private String alias;

  @Column(name = "timezone_utc", nullable = false)
  private Integer timezoneUTC;

  @Column(name = "latitude", nullable = false, length = 32)
  private String latitude;

  @Column(name = "longitude", nullable = false, length = 32)
  private String longitude;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "city_id", nullable = false)
  private City city;

  @Enumerated(EnumType.STRING)
  @Column(name = "state", nullable = false, length = 24)
  private AirportState state;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;
}
