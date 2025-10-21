package com.system.morapack.dao.morapack_psql.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "tracking_event",
       indexes = {
         @Index(name = "idx_te_city", columnList = "city_id"),
         @Index(name = "idx_te_order", columnList = "order_id"),
         @Index(name = "idx_te_segment", columnList = "segment_id"),
         @Index(name = "idx_te_timestamp", columnList = "event_timestamp")
       })
public class TrackingEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_event", nullable = false)
  private Integer id;

  @Column(name = "event_timestamp", nullable = false)
  private LocalDateTime timestamp;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 64)
  private TrackingEventType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private City city;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "segment_id")
  private Segment segment;
}
