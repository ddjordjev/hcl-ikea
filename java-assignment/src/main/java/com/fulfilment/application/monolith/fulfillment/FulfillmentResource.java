package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import org.jboss.logging.Logger;

@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfillmentResource {

  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private static final Logger LOGGER = Logger.getLogger(FulfillmentResource.class);

  @Inject FulfillmentRepository fulfillmentRepository;
  @Inject WarehouseRepository warehouseRepository;
  @Inject ProductRepository productRepository;

  @GET
  public List<FulfillmentAssignment> listAll() {
    return fulfillmentRepository.listAll();
  }

  @GET
  @Path("store/{storeId}")
  public List<FulfillmentAssignment> listByStore(Long storeId) {
    LOGGER.debugf("Fetching assignments for store %d", storeId);
    return fulfillmentRepository.findByStoreId(storeId);
  }

  @GET
  @Path("warehouse/{businessUnitCode}")
  public List<FulfillmentAssignment> listByWarehouse(String businessUnitCode) {
    LOGGER.debugf("Fetching assignments for warehouse %s", businessUnitCode);
    return fulfillmentRepository.findByWarehouse(businessUnitCode);
  }

  @GET
  @Path("product/{productId}")
  public List<FulfillmentAssignment> listByProduct(Long productId) {
    LOGGER.debugf("Fetching assignments for product %d", productId);
    return fulfillmentRepository.findByProductId(productId);
  }

  @POST
  @Transactional
  public Response create(FulfillmentAssignment assignment) {
    if (assignment == null) {
      throw new WebApplicationException("Fulfillment assignment payload is required.", 422);
    }
    if (assignment.warehouseBusinessUnitCode == null || assignment.warehouseBusinessUnitCode.isBlank()) {
      throw new WebApplicationException("warehouseBusinessUnitCode is required.", 422);
    }
    if (assignment.productId == null) {
      throw new WebApplicationException("productId is required.", 422);
    }
    if (assignment.storeId == null) {
      throw new WebApplicationException("storeId is required.", 422);
    }

    if (warehouseRepository.findByBusinessUnitCode(assignment.warehouseBusinessUnitCode) == null) {
      throw new WebApplicationException(
          "Warehouse with businessUnitCode '" + assignment.warehouseBusinessUnitCode + "' not found.", 404);
    }
    if (productRepository.findById(assignment.productId) == null) {
      throw new WebApplicationException("Product with id " + assignment.productId + " not found.", 404);
    }
    if (Store.findById(assignment.storeId) == null) {
      throw new WebApplicationException("Store with id " + assignment.storeId + " not found.", 404);
    }

    if (fulfillmentRepository.assignmentExists(
        assignment.warehouseBusinessUnitCode, assignment.productId, assignment.storeId)) {
      throw new WebApplicationException("This fulfillment assignment already exists.", 409);
    }

    enforceConstraints(assignment);

    assignment.createdAt = LocalDateTime.now();
    fulfillmentRepository.persist(assignment);

    LOGGER.infof("Created assignment: warehouse=%s product=%d store=%d",
        assignment.warehouseBusinessUnitCode, assignment.productId, assignment.storeId);

    return Response.ok(assignment).status(201).build();
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    FulfillmentAssignment entity = fulfillmentRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Fulfillment assignment with id " + id + " does not exist.", 404);
    }
    fulfillmentRepository.delete(entity);
    LOGGER.infof("Deleted assignment %d", id);
    return Response.status(204).build();
  }

  private void enforceConstraints(FulfillmentAssignment assignment) {
    long warehousesForProduct =
        fulfillmentRepository.countWarehousesForProductAtStore(
            assignment.productId, assignment.storeId);
    if (warehousesForProduct >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new WebApplicationException(
          "Product " + assignment.productId + " already has "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses fulfilling it for store " + assignment.storeId
              + ". Maximum reached.", 422);
    }

    boolean isNewWarehouseForStore =
        !fulfillmentRepository.warehouseFulfillsStore(
            assignment.warehouseBusinessUnitCode, assignment.storeId);

    if (isNewWarehouseForStore) {
      long warehousesForStore =
          fulfillmentRepository.countDistinctWarehousesForStore(assignment.storeId);
      if (warehousesForStore >= MAX_WAREHOUSES_PER_STORE) {
        throw new WebApplicationException(
            "Store " + assignment.storeId + " already has "
                + MAX_WAREHOUSES_PER_STORE + " warehouses fulfilling it. Maximum reached.", 422);
      }
    }

    if (!fulfillmentRepository.warehouseStoresProduct(
            assignment.warehouseBusinessUnitCode, assignment.productId)) {
      long productsInWarehouse =
          fulfillmentRepository.countDistinctProductsForWarehouse(
              assignment.warehouseBusinessUnitCode);
      if (productsInWarehouse >= MAX_PRODUCTS_PER_WAREHOUSE) {
        throw new WebApplicationException(
            "Warehouse '" + assignment.warehouseBusinessUnitCode + "' already stores "
                + MAX_PRODUCTS_PER_WAREHOUSE + " product types. Maximum reached.", 422);
      }
    }
  }
}
