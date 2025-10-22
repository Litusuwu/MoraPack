package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSchema {
    private Integer id;
    private String description;
    private String status;
    private LocalDateTime generationDate;
    private Integer orderId;
    private String orderName;
}
