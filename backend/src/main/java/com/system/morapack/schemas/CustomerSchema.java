package com.system.morapack.schemas;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerSchema {
    private int id;
    private String name;
    private String email;
    private CitySchema deliveryCitySchema;
}
