# SE423 — Milestone 2 Construction Report

**SeatWise — Event Ticketing and Seat Reservation System**  
**Team Lead:** Fatima Sultan (2023206)

| Role | Name | Reg. No. | Email |
| --- | --- | --- | --- |
| Member 1 | Chaudhry Ahmed Ali Khan | 2023167 | u2023167@giki.edu.pk |
| Member 2 (Team Lead) | Fatima Sultan | 2023206 | u2023206@giki.edu.pk |
| Member 3 | Abdul Hakeem | 2023007 | u2023007@giki.edu.pk |

This is our Milestone 2 write-up for SE423. At M1 we proposed the design. Now the core actually runs: hold, pay, decline, expire, and a race for one seat.

## 1. What works in this build

We ran `mvn clean verify` on this tree. Result: **BUILD SUCCESS**, **15 tests**, **0 failures**. Line coverage from JaCoCo is about **89.7%** (gate is 60%). Checkstyle: **0 violations**.

Non-interactive demo:

```bash
mvn -q package
java -jar target/seatwise.jar --demo
```

What the demo shows (and what we will point at in class):

| Outcome | What you see |
| --- | --- |
| Hold + pay | Booking ends as CONFIRMED |
| Bad card (`FAIL` / `0000`) | PAYMENT FAILED; seat back to AVAILABLE |
| Timed hold | EXPIRED after the clock advances |
| 8 threads, one seat | `RACE winners=1 losers=7` |

## 2. Modules that are actually implemented

| Module | Main classes | Owner |
| --- | --- | --- |
| Event Catalog | `Event.Builder`, `SeatMapGenerator`, `PriceCalculator`, `EventService` | Chaudhry Ahmed Ali Khan |
| Booking & Inventory | `Booking` + states, `SeatInventory`, `BookingService`, `HoldExpiryScheduler` | Fatima Sultan (Lead) |
| Payments | `PaymentGateway`, `ScriptedPaymentGateway`, `PaymentService` | Abdul Hakeem |
| Notifications | `NotificationService` (listens to booking events) | Abdul Hakeem |
| Checkout | `CheckoutService` (facade) | Fatima Sultan (integration) |
| Persistence | SQLite `schema.sql` + JDBC repositories | Shared; Fatima integrates |
| Application | `SeatWiseApp`, console, `--demo` | Shared |

Checkout is not a fifth “business” module stealing work from the other four. It is the purchase facade so the console does not wire catalog + booking + payment by itself. That matches what we said in M1.

## 3. Patterns in the running code

| Pattern | Kind | Where | Why it is real, not just a name on a slide |
| --- | --- | --- | --- |
| Builder | Creational | `Event.Builder` | Rejects bad sales windows and duplicate section names in one place |
| State | Behavioral | `PendingState`, `HeldState`, `ConfirmedState`, … | `confirm()` on a pending booking throws `InvalidStateTransitionException` |
| Observer | Behavioral | `BookingEventPublisher`, `NotificationService` | `com.seatwise.booking` does not import `com.seatwise.notification` |
| Strategy | Behavioral | `PaymentGateway` | Tests inject `DecliningPaymentGateway` without editing `PaymentService` |
| Repository | Port | `*Repository` interfaces | In-memory fakes and JDBC both implement the same ports |
| Facade | Structural | `CheckoutService` | Quote → hold → pay → roll back lives in one use case |

Alternatives we rejected are still in the M1 proposal (long constructors, `switch(status)` everywhere, booking calling notifications directly, etc.).

## 4. SOLID — where to open the file

| Principle | What to show |
| --- | --- |
| SRP | Pricing is in `PriceCalculator`, not inside `BookingService` |
| OCP | New status = new state class; `BookingStates.of` is the only status switch |
| LSP | Scripted and Declining gateways both return `GatewayReceipt` |
| ISP | `PaymentRepository` only has save/lookup — not seat queries |
| DIP | `BookingService` takes repository/publisher interfaces, not JDBC classes |

## 5. Defensive programming

`Guard` checks null, blank, email, ranges and simple business rules at service edges. Failures use typed exceptions:

- `ValidationException`
- `EntityNotFoundException`
- `SeatUnavailableException`
- `InvalidStateTransitionException`
- `PaymentFailedException`
- `PersistenceException`

The console prints `errorCode()` and the message. If a notification listener throws, the booking still finishes. The hold-expiry thread catches a failed sweep and keeps going.

## 6. Concurrency (same story as the code)

1. `SeatInventory` — one `ReentrantLock` per event. Multi-seat hold is check-then-write under that lock. If any seat is taken, none change.
2. `BookingService` — one lock per booking id so confirm and expire cannot both win.
3. Lock order: booking lock, then inventory lock. Inventory never calls back into booking.
4. `AtomicLong` for hold attempts / conflicts.
5. `Money` is immutable.
6. Listeners live in a `CopyOnWriteArrayList`.
7. `AsyncEventPublisher` sends notifications off the booking thread.
8. If SQLite fails while saving a hold, inventory is released. If confirm fails to persist, seats go from BOOKED back to HELD.

## 7. Tests we rely on for M2

```bash
mvn test
```

| Requirement | Test method |
| --- | --- |
| SW-FR-01 (no double booking) | `eightThreadsRacingForOneSeatProduceExactlyOneWinner` |
| SW-FR-02 (expiry) | `secondCustomerCannotTakeAHeldSeatAndExpiredHoldFreesIt` |
| SW-FR-03 (decline) | `declinedPaymentReleasesTheHold` |
| SW-FR-04 (restart) | `sqliteRoundTripSurvivesRestart` |
| SW-FR-05 (money) | `moneyRoundsAndRejectsGarbage`, `eventBuilderValidatesAndSeatMapIsPriced` |
| SW-FR-06 (illegal transition) | `bookingStateMachineRejectsIllegalMoves` |

For the live demo we also open:

1. `Event.java` — Builder checks  
2. `HeldState.java` — which moves are legal  
3. `SeatWiseApp.wire` — notification listener subscribed from outside booking  
4. `PaymentGateway.java` + declining gateway test  
5. `SeatInventory` + `BookingService` — the two locks  
6. `git log` — keep a failing-test commit and the fix commit separate (do not squash)

## 8. Who explains what in Q&A

| Person | Be ready to open and explain |
| --- | --- |
| Chaudhry Ahmed Ali Khan | `Event.java`, `EventService.java`, `JdbcEventRepository.java` |
| Fatima Sultan | `BookingService.java`, `SeatInventory.java`, `HoldExpiryScheduler.java`, `CheckoutService.java` |
| Abdul Hakeem | `PaymentService.java`, `NotificationService.java`, gateway strategy / decline path |

## 9. Scope note

M2 is the working core + design evidence. Final polish, more demo scripting, and the finished SQAP/GQM package belong to M3. We are not claiming those here.

Prepared by Chaudhry Ahmed Ali Khan, Fatima Sultan, and Abdul Hakeem.  
Team Lead: Fatima Sultan.
