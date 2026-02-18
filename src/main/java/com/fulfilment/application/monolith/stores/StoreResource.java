package com.fulfilment.application.monolith.stores;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

import static jakarta.transaction.Status.STATUS_COMMITTED;

@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

    @Inject
    LegacyStoreManagerGateway legacyStoreManagerGateway;
    @Inject
    TransactionSynchronizationRegistry txSyncRegistry;

    private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

    @GET
    public List<Store> get() {
        return Store.listAll(Sort.by("name"));
    }

    @GET
    @Path("{id}")
    public Store getSingle(Long id) {
        LOGGER.debugf("Fetching store %d", id);
        Store entity = Store.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @POST
    @Transactional
    public Response create(Store store) {
        if (store.id != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }

        store.persist();
        LOGGER.infof("Created store %s", store.name);

        runAfterCommit(() -> legacyStoreManagerGateway.createStoreOnLegacySystem(store));

        return Response.ok(store).status(201).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Store update(Long id, Store updatedStore) {
        if (updatedStore.name == null) {
            throw new WebApplicationException("Store Name was not set on request.", 422);
        }

        Store entity = Store.findById(id);

        if (entity == null) {
            throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
        }

        entity.name = updatedStore.name;
        entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
        LOGGER.infof("Updated store %d", id);

        runAfterCommit(() -> legacyStoreManagerGateway.updateStoreOnLegacySystem(entity));

        return entity;
    }

    @PATCH
    @Path("{id}")
    @Transactional
    public Store patch(Long id, Store updatedStore) {
        Store entity = Store.findById(id);

        if (entity == null) {
            throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
        }

        if (updatedStore.name != null) {
            entity.name = updatedStore.name;
        }

        entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
        LOGGER.infof("Patched store %d", id);

        runAfterCommit(() -> legacyStoreManagerGateway.updateStoreOnLegacySystem(entity));

        return entity;
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(Long id) {
        Store entity = Store.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
        }
        entity.delete();
        LOGGER.infof("Deleted store %d", id);

        runAfterCommit(() -> legacyStoreManagerGateway.updateStoreOnLegacySystem(entity));

        return Response.status(204).build();
    }

    private void runAfterCommit(Runnable action) {
        txSyncRegistry.registerInterposedSynchronization(
                new Synchronization() {
                    @Override
                    public void beforeCompletion() {
                        // no-op
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            try {
                                action.run();
                            } catch (Exception e) {
                                LOGGER.error("Legacy call failed after commit", e);
                            }
                        }
                    }
                });
    }
}
