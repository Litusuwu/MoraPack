package com.system.morapack.dao.morapack_psql.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;
  
  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "weight", nullable = false)
  private Double weight;

  @Column(name = "volume", nullable = false)
  private Double volume;

  @Column(name = "creation_date", nullable = false)
  @CreationTimestamp
  private LocalDateTime creationDate;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;
}
