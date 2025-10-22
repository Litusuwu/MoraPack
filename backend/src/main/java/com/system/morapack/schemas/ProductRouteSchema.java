package com.system.morapack.schemas;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRouteSchema {
    private Integer productId;
    private Integer orderId;
    private String orderName;
    private List<FlightSchema> flights;
    private String originCity;
    private String destinationCity;
    private Integer flightCount;
}
