package com.system.morapack.dao.morapack_psql.model;



import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "performance_metric",
       indexes = {
         @Index(name = "idx_metric_plan",      columnList = "plan_id"),
         @Index(name = "idx_metric_event",     columnList = "event_id"),
         @Index(name = "idx_metric_scenario",  columnList = "scenario_id"),
         @Index(name = "idx_metric_algorithm", columnList = "algorithm_id")
       })
public class PerformanceMetric {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "value", nullable = false)
  private Double value;

  @Column(name = "period", nullable = false, length = 64)
  private String period;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id")
  private Plan plan;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id")
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scenario_id")
  private Scenario scenario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "algorithm_id")
  private Algorithm algorithm;
}