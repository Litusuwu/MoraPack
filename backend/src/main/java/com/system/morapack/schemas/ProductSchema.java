package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSchema {
    private Integer id;
    private String name;
    private Double weight;
    private Double volume;
    private LocalDateTime creationDate;
    private Integer orderId;
    private StringBuilder assignedFlight;
    private Status status;
}
