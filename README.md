# SeatWise

Event ticketing and seat reservation system for the joint SE423 (Software Construction) and SE431 (Software Quality Assurance) project.

A customer can browse a published event, hold seats for a limited time, pay, and receive a confirmation. If two people request the same seat, only one hold succeeds. If payment is declined or the hold expires, the seats go back on sale.

## Modules

| Module | Package | What it does |
| --- | --- | --- |
| Event catalog | `com.seatwise.catalog` | Venues, events, sections, seat-map generation, pricing |
| Booking | `com.seatwise.booking` | Holds, confirmation, cancellation, expiry, thread-safe inventory |
| Payments | `com.seatwise.payment` | Charge a hold through a pluggable gateway and store the attempt |
| Notifications | `com.seatwise.notification` | Email, SMS and in-app messages when a booking changes |
| Checkout | `com.seatwise.checkout` | One purchase flow across catalog, booking and payment |
| Persistence | `com.seatwise.persistence` | SQLite schema and JDBC adapters |
| Application | `com.seatwise.app` | Wiring, console UI, and the non-interactive demo |

## Requirements

- JDK 17 or newer (`java -version`)
- Maven 3.9+ (`mvn -version`)

## Run the tests and quality gates

```bash
cd ProjectSE
mvn clean verify
```

This compiles the code, runs JUnit, enforces at least 60% line coverage (JaCoCo), and runs Checkstyle. The HTML coverage report is written to `target/site/jacoco/index.html`.

## Run the program

Interactive menu (data is stored in `data/seatwise.db`):

```bash
mvn -q package
java -jar target/seatwise.jar
```

Non-interactive demo used in class and in CI:

```bash
java -jar target/seatwise.jar --demo
```

Use another database file:

```bash
java -jar target/seatwise.jar --db data/my-demo.db
java -jar target/seatwise.jar --demo --db data/my-demo.db
```

In the menu, seat labels look like `VIP-A-1,VIP-A-2`. A card or account reference that contains `FAIL` or ends with `0000` is declined on purpose, so you can show the failure path.

## Design patterns

| Pattern | Where | Problem it solves |
| --- | --- | --- |
| Builder (creational) | `Event.Builder` | An event has many fields and cross-field rules |
| State (behavioral) | `booking.state` | Legal booking transitions live in one class per status |
| Observer (behavioral) | `BookingEventPublisher`, `NotificationService` | Booking does not depend on notification code |
| Strategy (behavioral) | `PaymentGateway` | The charge algorithm can be swapped without editing checkout |
| Repository | `*Repository` interfaces | Business code does not depend on JDBC |
| Facade | `CheckoutService` | The UI calls one purchase flow instead of three modules |

## Concurrency

- `SeatInventory` locks one event at a time so a multi-seat hold is all-or-nothing.
- `BookingService` locks one booking at a time so confirm and expire cannot interleave.
- Lock order is booking lock, then inventory lock. Inventory never calls back into booking.
- `HoldExpiryScheduler` runs on its own daemon thread.
- Notifications are published on a background executor.
- `Money` is immutable, so it needs no lock.

## Repository layout

- `main` is the release branch.
- `develop` is the integration branch.
- Feature work goes on `feature/<short-name>` and is merged into `develop` after tests pass.
- Do not squash-merge if you need the red-green-refactor commit history to stay visible.

## Documents for the instructor

See `docs/DEMO_SCRIPT.md` for the order to present the work, and `docs/M1`, `docs/M2`, `docs/M3` for the milestone write-ups.

**Team:** Chaudhry Ahmed Ali Khan (2023167), Fatima Sultan (2023206, Team Lead), Abdul Hakeem (2023007).

**Milestone 2 uploads:** `docs/M2/SE423_M2_Construction.docx`, `docs/M2/SE431_M2_Inspection_Tests_SCM.docx`, `docs/M2/AI_Usage_Log.docx`, plus hosted repo link. Fill the external Fagan inspector name before SE431 upload.
