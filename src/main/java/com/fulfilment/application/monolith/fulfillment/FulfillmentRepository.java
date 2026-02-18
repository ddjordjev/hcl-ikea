package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;

@ApplicationScoped
public class FulfillmentRepository implements PanacheRepository<FulfillmentAssignment> {

    @Inject
    EntityManager em;

    public List<FulfillmentAssignment> findByStoreId(Long storeId) {
        return list("storeId", storeId);
    }

    public List<FulfillmentAssignment> findByWarehouse(String warehouseBusinessUnitCode) {
        return list("warehouseBusinessUnitCode", warehouseBusinessUnitCode);
    }

    public List<FulfillmentAssignment> findByProductId(Long productId) {
        return list("productId", productId);
    }

    public long countWarehousesForProductAtStore(Long productId, Long storeId) {
        return em.createQuery(
                        "select count(distinct f.warehouseBusinessUnitCode) from FulfillmentAssignment f "
                                + "where f.productId = :productId and f.storeId = :storeId",
                        Long.class)
                .setParameter("productId", productId)
                .setParameter("storeId", storeId)
                .getSingleResult();
    }

    public long countDistinctWarehousesForStore(Long storeId) {
        return em.createQuery(
                        "select count(distinct f.warehouseBusinessUnitCode) from FulfillmentAssignment f "
                                + "where f.storeId = :storeId",
                        Long.class)
                .setParameter("storeId", storeId)
                .getSingleResult();
    }

    public long countDistinctProductsForWarehouse(String warehouseBusinessUnitCode) {
        return em.createQuery(
                        "select count(distinct f.productId) from FulfillmentAssignment f "
                                + "where f.warehouseBusinessUnitCode = :buCode",
                        Long.class)
                .setParameter("buCode", warehouseBusinessUnitCode)
                .getSingleResult();
    }

    public boolean assignmentExists(
            String warehouseBusinessUnitCode, Long productId, Long storeId) {
        return count(
                "warehouseBusinessUnitCode = ?1 and productId = ?2 and storeId = ?3",
                warehouseBusinessUnitCode,
                productId,
                storeId)
                > 0;
    }

    public boolean warehouseFulfillsStore(String warehouseBusinessUnitCode, Long storeId) {
        return count(
                "warehouseBusinessUnitCode = ?1 and storeId = ?2",
                warehouseBusinessUnitCode,
                storeId)
                > 0;
    }

    public boolean warehouseStoresProduct(String warehouseBusinessUnitCode, Long productId) {
        return count(
                "warehouseBusinessUnitCode = ?1 and productId = ?2",
                warehouseBusinessUnitCode,
                productId)
                > 0;
    }
}
