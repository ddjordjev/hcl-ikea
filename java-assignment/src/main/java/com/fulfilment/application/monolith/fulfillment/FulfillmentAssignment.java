package com.fulfilment.application.monolith.fulfillment;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fulfillment_assignment",
        uniqueConstraints =
        @UniqueConstraint(
                name = "uq_warehouse_product_store",
                columnNames = {"warehouseBusinessUnitCode", "productId", "storeId"}))
@Cacheable
public class FulfillmentAssignment {

    @Id
    @GeneratedValue
    public Long id;

    @Column(nullable = false)
    public String warehouseBusinessUnitCode;

    @Column(nullable = false)
    public Long productId;

    @Column(nullable = false)
    public Long storeId;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    public FulfillmentAssignment() {
    }
}
