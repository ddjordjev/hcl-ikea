package com.fulfilment.application.monolith.warehouses.domain.models;

public class Location {
    public String identification;

    public int maxNumberOfWarehouses;

    public int maxCapacity;

    public Location(String identification, int maxNumberOfWarehouses, int maxCapacity) {
        this.identification = identification;
        this.maxNumberOfWarehouses = maxNumberOfWarehouses;
        this.maxCapacity = maxCapacity;
    }
}
