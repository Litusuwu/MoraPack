package com.system.morapack.schemas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Warehouse {
    private int id;
    private Airport airport;
    private int maxCapacity;
    private int usedCapacity;
    private String name;
    private boolean isMainWarehouse;
}
