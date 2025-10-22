package com.system.morapack.schemas;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSchema {
    private Integer id;
    private String phone;
    private String fiscalAddress;
    private LocalDateTime createdAt;
    private Integer personId;
    private String personName;
    private String personLastName;

    // Legacy fields for algorithm compatibility
    private String name;
    private String email;
    private CitySchema deliveryCitySchema;
}
