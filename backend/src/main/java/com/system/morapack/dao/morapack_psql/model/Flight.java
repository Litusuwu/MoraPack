package com.system.morapack.dao.morapack_psql.model;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "flights")
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "frequencyPerDay", nullable = false)
    private Double frequencyPerDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_airport_id", nullable = false)
    private Airport originAirport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_airport_id", nullable = false)
    private Airport destinationAirport;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "used_capacity", nullable = false)
    private Integer usedCapacity;

    @Column(name = "transport_time", nullable = false)
    private Double transportTime;

    @Column(name = "cost", nullable = false)
    private Double cost;




}
