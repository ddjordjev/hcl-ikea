# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. 
The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. 
Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

I don't have relatable experience with this problem, so I would accept few of the suggestions given to me by our AI overlords, 
and I also ask some questions to understand the problem better.

It looks like the main challenge here is **data modeling** — how to structure cost records so they can be sliced and attributed flexibly.

Some of the questions would be:
- what is required granularity (Warehouse, Store, Order)?
- what is the cost type and categorization (labor, inventory, transportation, overhead)?
- how to track historical data and is there an existing ERP or accounting system? How to integrate with any existing cost data?
- how to allocate costs? What is shared resource allocation logic, indirect cost distribution?
- should these calculations be event-driven or scheduled calculations?
- how often allocation rules change (i.e. rule engine or simpler config-driven logic)

In order to implement any of cost tracking and allocation logic, current code base would need some extensions
- A `CostEntry` entity linked to the Business Unit Code (and optionally Store/Product), recording cost type, amount, period, and which allocation method was used. 
- Allocation logic behind an interface (e.g. `CostAllocationStrategy`) so different methods can be swapped without touching the core domain. 
The exact strategies would need to come from the business side.


## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. 
The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. 
How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

Any optimization strategy depends on having the right data to measure against.

Few areas where the current system architecture could support cost optimization:
- **Warehouse-to-Store assignment.** If the system tracked shipping costs per assignment, it would be possible to model alternative assignments and estimate savings. 
- **Capacity utilization visibility.** The Warehouse entity tracks `capacity` and `stock`. Exposing utilization metrics (stock/capacity ratio) per Warehouse via an API or dashboard could help identify under-utilized locations. 
Operations team could use this information.
- **Replacement timing support.** The "replace" operation already exists. 
Adding cost history per Business Unit Code would give decision-makers the data to evaluate when a replacement makes financial sense. 
The system could surface things like "average cost trend over last N months" to support that decision.

From an engineering side, the key is to build the measurement and reporting capabilities first, so that any optimization strategy can be evaluated against a baseline.

Reporting should bring "Cost visibility" - End-to-end visibility across fulfillment lifecycle and help identify top cost drivers.

Few other implementation related questions:
1. What data is currently available about shipping costs and delivery times (i.e. is this more data modeling problem or a data collection problem).
2. Are there SLAs or constraints on delivery time that would limit how Warehouse-to-Store assignments can be reshuffled? 
3. Are the max-warehouses-per-location limits driven by physical/regulatory constraints, or are they configurable business rules?



## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. 
The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. 
What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

This is essentially a system integration problem.

 Some important benefits would be:
 - single source of truth and real-time cost control. 
 - compliance and reporting are also greatly improved.
 - reduced manual reconciliation and improved governance.

When it comes to integration approach, initially there should be 2 separate concerns:
- migration of existing data from the fulfillment system to the financial system.
- real-time cost data synchronization.

Migration should be done in a way that minimizes downtime and minimizes the risk of data loss. 
Batches are preferred here and some form of reconciliation should be implemented to ensure data integrity.

For real-time synchronization, event-driven integration is preferred. 
For instance current codebase already has a pattern for post-commit side effects — `StoreResource` uses `runAfterCommit` to notify a legacy system only after the transaction succeeds. 
The same pattern could be generalized: when a Warehouse is created, replaced, or archived, publish a domain event. 
A message broker (Kafka, RabbitMQ) or a transactional outbox table would decouple the fulfillment system from the financial system, so neither blocks the other.

Reconciliation here is also required (e.g. as a nightly batch job). It should compare key aggregates between the two systems and raise alerts on mismatches. 
Event-driven systems are reliable but not infallible — a reconciliation job catches the edge cases.

With migrations there is always a problem with schema contracts. Both systems need to agree on the event payload shape.

Few other questions to consider:
- which financial system is in use (SAP, Oracle Financials, custom) and does it expose an API for ingestion, or does it expect file-based imports?
- what latency is acceptable? 



## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. 
The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take 
into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

I don't have experience with financial forecasting specifically.
But from the look of it, the key question is: what data does the system need to capture and expose so that whoever does the forecasting has reliable inputs?

What needs to be done or considered:
- **Forecasting Inputs** - historical cost data, actual cost data, sales projections, demand projections, inflation, fuel prices, labor market trends, etc.
- **Budget vs. Actual comparison.** Once a budget is set, the system could compare incoming cost actuals against it and flag variances. 
This is relatively straightforward CRUD + threshold logic, but the thresholds and escalation rules would need to come from the business side.
- **System Design Considerations** Versioned budgets, scenario modeling, BI integration, what-if analysis.

Possible questions of interest might also be:
- who actually does the forecasting today? Is there a planning team, or is this expected to be built from scratch?
- what granularity is needed — per BU Code, per location, per product line?
- is there an existing BI or analytics tool in place, or should this system expose raw data via APIs for an external tool to consume?


## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. 
The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. 
Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

As for importance, the following comes to mind:
- audit traceability and compliance
- accurate reporting
- cost history preservation
- enables benchmarking
- trend analysis

In terms of keeping the new Warehouse operation within budget, cost history can primarily serve as a benchmark. 

Some other benefits of preserving cost history would be:
- it can be used to trigger early warnings and prevent unintended overspending. 
- the old warehouse's cost breakdown (labor, rent, utilities, transport) can serve as the budget template for the new one. 
- cost history reveals seasonal patterns and spikes.
- help with post-replacement cost monitoring.

Some other questions to consider:
- how long does a physical Warehouse transition typically take? 
- is cost history something that needs to be exposed through the API (for programmatic access), or is a reporting dashboard enough?
- are there specific retention requirements for cost data (e.g. regulatory or tax-related)?

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
