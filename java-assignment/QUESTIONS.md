# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would definitely strive toward a single consistent strategy and do the refactoring.

As per analysis there are three different approaches being used:

a) Active Record (Store) — Store extends PanacheEntity and calls static methods like
   Store.findById() / Store.listAll() directly from the resource layer. This is convenient for
   simple CRUD but tightly couples the JPA entity to persistence operations and makes unit-testing
   the resource in isolation difficult (you cannot easily mock the static calls).

b) Repository Pattern via Panache (Product, FulfillmentAssignment) — ProductRepository and
   FulfillmentRepository implement PanacheRepository<T>, giving a proper injectable, mockable
   layer. This is a step forward because the resource depends on an injected repository rather
   than static calls.

c) Hexagonal / Ports-and-Adapters (Warehouse) — The Warehouse domain has its own domain model
   (domain.models.Warehouse), port interfaces (WarehouseStore, LocationResolver), use-case classes,
   and an adapter layer (WarehouseRepository / DbWarehouse) that translates between the domain
   model and the JPA entity. This gives the strongest separation of concerns: the domain logic
   has zero dependency on JPA or Quarkus.

My personal preference and experience leans toward approach (b), with KISS approach in mind, but my AI friend suggests 
that (c) is more robust and maintainable in the long run, especially for complex domains, so I would strongly consider that approach as well.

Problem with any complex "nice to have and sounds good approach" is that they all sound good at the beginning, but at the end what usually happens 
is that no one is using them and they are just a maintenance burden.

In my opinion you should always start with the simplest approach and only add complexity when you have a good reason to do so.

```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, 
but for the other endpoints - `Product` and `Store` - we just coded directly everything. 
What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Same as previous one, OpenAPI adds complexity which generally makes things harder to develop, debug and maintain.

But it is worth considering, or better yet it's mandatory, when you have a strong reason to do so (i.e. production system with multiple consumers).

On the other hand, for internal or purely CRUD endpoints with a single consumer, 
code-first with quarkus-smallrye-openapi annotations to auto-generate the spec is a practical middle ground — you still get a spec, 
but without the overhead of the code-generation pipeline.

```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? 
Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt

There should always be Domain unit tests (e.g. CreateWarehouseUseCaseTest, ReplaceWarehouseUseCaseTest, ArchiveWarehouseUseCaseTest). 
These are the highest-value tests because they validate core business rules in isolation — no database, no HTTP server, sub-second execution.
And these need to be written and done during the development process.

Next in line are REST endpoint integration tests (e.g. FulfillmentEndpointTest, ProductEndpointTest). These exercise the full HTTP → Resource → Use Case → Database 
stack using @QuarkusTest + REST Assured. They catch wiring issues (CDI injection, @Transactional boundaries, JSON serialization) that unit tests cannot.
They should be smaller in numbers of course but in my opinion they are extremely valuable and often are able to catch bugs that unit tests cannot.

Last in line, priority and numbers wise, should be full blown integration tests (@QuarkusIntegrationTest like WarehouseEndpointIT).
In my experience these are often almost impossible to write and maintain, especially when the system is complex and has many dependencies.
They usually end up always being red in CI, with everybody ignoring them. But it's a great feeling when you see those green in the pipeline.

```