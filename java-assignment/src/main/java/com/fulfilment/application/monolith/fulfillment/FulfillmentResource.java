package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
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
    return fulfillmentRepository.findByStoreId(storeId);
  }

  @GET
  @Path("warehouse/{businessUnitCode}")
  public List<FulfillmentAssignment> listByWarehouse(String businessUnitCode) {
    return fulfillmentRepository.findByWarehouse(businessUnitCode);
  }

  @GET
  @Path("product/{productId}")
  public List<FulfillmentAssignment> listByProduct(Long productId) {
    return fulfillmentRepository.findByProductId(productId);
  }

  @POST
  @Transactional
  public Response create(FulfillmentAssignment assignment) {
    validatePayload(assignment);
    validateReferencedEntitiesExist(assignment);
    checkDuplicateAssignment(assignment);
    enforceConstraints(assignment);

    assignment.createdAt = LocalDateTime.now();
    fulfillmentRepository.persist(assignment);

    LOGGER.infof(
        "Fulfillment assignment created: Warehouse [%s] -> Product [%d] -> Store [%d]",
        assignment.warehouseBusinessUnitCode, assignment.productId, assignment.storeId);

    return Response.ok(assignment).status(201).build();
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    FulfillmentAssignment entity = fulfillmentRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException(
          "Fulfillment assignment with id " + id + " does not exist.", 404);
    }

    fulfillmentRepository.delete(entity);

    LOGGER.infof("Fulfillment assignment %d deleted.", id);
    return Response.status(204).build();
  }

  private void validatePayload(FulfillmentAssignment assignment) {
    if (assignment == null) {
      throw new WebApplicationException("Fulfillment assignment payload is required.", 422);
    }
    if (assignment.warehouseBusinessUnitCode == null
        || assignment.warehouseBusinessUnitCode.isBlank()) {
      throw new WebApplicationException("warehouseBusinessUnitCode is required.", 422);
    }
    if (assignment.productId == null) {
      throw new WebApplicationException("productId is required.", 422);
    }
    if (assignment.storeId == null) {
      throw new WebApplicationException("storeId is required.", 422);
    }
  }

  private void validateReferencedEntitiesExist(FulfillmentAssignment assignment) {
    if (warehouseRepository.findByBusinessUnitCode(assignment.warehouseBusinessUnitCode) == null) {
      throw new WebApplicationException(
          "Warehouse with businessUnitCode '"
              + assignment.warehouseBusinessUnitCode
              + "' not found.",
          404);
    }

    Product product = productRepository.findById(assignment.productId);
    if (product == null) {
      throw new WebApplicationException(
          "Product with id " + assignment.productId + " not found.", 404);
    }

    Store store = Store.findById(assignment.storeId);
    if (store == null) {
      throw new WebApplicationException(
          "Store with id " + assignment.storeId + " not found.", 404);
    }
  }

  private void checkDuplicateAssignment(FulfillmentAssignment assignment) {
    if (fulfillmentRepository.assignmentExists(
        assignment.warehouseBusinessUnitCode, assignment.productId, assignment.storeId)) {
      throw new WebApplicationException(
          "This fulfillment assignment already exists.", 409);
    }
  }

  private void enforceConstraints(FulfillmentAssignment assignment) {
    // Constraint 1: Each Product can be fulfilled by max 2 different Warehouses per Store
    if (!fulfillmentRepository.warehouseFulfillsStore(
            assignment.warehouseBusinessUnitCode, assignment.storeId)
        || !isWarehouseAlreadyFulfillingProductAtStore(assignment)) {
      long warehousesForProductAtStore =
          fulfillmentRepository.countWarehousesForProductAtStore(
              assignment.productId, assignment.storeId);

      boolean isNewWarehouseForThisProductAtStore =
          !fulfillmentRepository.assignmentExists(
              assignment.warehouseBusinessUnitCode, assignment.productId, assignment.storeId);

      if (isNewWarehouseForThisProductAtStore
          && warehousesForProductAtStore >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
        throw new WebApplicationException(
            "Product "
                + assignment.productId
                + " already has "
                + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
                + " warehouses fulfilling it for store "
                + assignment.storeId
                + ". Maximum reached.",
            422);
      }
    }

    // Constraint 2: Each Store can be fulfilled by max 3 different Warehouses
    boolean isNewWarehouseForStore =
        !fulfillmentRepository.warehouseFulfillsStore(
            assignment.warehouseBusinessUnitCode, assignment.storeId);

    if (isNewWarehouseForStore) {
      long distinctWarehousesForStore =
          fulfillmentRepository.countDistinctWarehousesForStore(assignment.storeId);

      if (distinctWarehousesForStore >= MAX_WAREHOUSES_PER_STORE) {
        throw new WebApplicationException(
            "Store "
                + assignment.storeId
                + " already has "
                + MAX_WAREHOUSES_PER_STORE
                + " warehouses fulfilling it. Maximum reached.",
            422);
      }
    }

    // Constraint 3: Each Warehouse can store max 5 types of Products
    boolean isNewProductForWarehouse =
        !fulfillmentRepository.warehouseStoresProduct(
            assignment.warehouseBusinessUnitCode, assignment.productId);

    if (isNewProductForWarehouse) {
      long distinctProductsForWarehouse =
          fulfillmentRepository.countDistinctProductsForWarehouse(
              assignment.warehouseBusinessUnitCode);

      if (distinctProductsForWarehouse >= MAX_PRODUCTS_PER_WAREHOUSE) {
        throw new WebApplicationException(
            "Warehouse '"
                + assignment.warehouseBusinessUnitCode
                + "' already stores "
                + MAX_PRODUCTS_PER_WAREHOUSE
                + " product types. Maximum reached.",
            422);
      }
    }
  }

  private boolean isWarehouseAlreadyFulfillingProductAtStore(FulfillmentAssignment assignment) {
    return fulfillmentRepository.assignmentExists(
        assignment.warehouseBusinessUnitCode, assignment.productId, assignment.storeId);
  }
}
