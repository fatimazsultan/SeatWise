# SE431 — Milestone 2 Inspection, Test Design and SCM

**SeatWise — Event Ticketing and Seat Reservation System**  
**Team Lead:** Fatima Sultan (2023206)

| Role | Name | Reg. No. | Email |
| --- | --- | --- | --- |
| Member 1 | Chaudhry Ahmed Ali Khan | 2023167 | u2023167@giki.edu.pk |
| Member 2 (Team Lead) | Fatima Sultan | 2023206 | u2023206@giki.edu.pk |
| Member 3 | Abdul Hakeem | 2023007 | u2023007@giki.edu.pk |

This is our SE431 Milestone 2 package. It covers Fagan-style inspection, static analysis, V&V, black-box and white-box test design, SCM evidence, an updated RTM, and risk notes after this build.

**Important:** fill in the external inspector name and the inspection date before you upload. The course wants one inspector from another team — not someone from our own group.

## 0. Quality ownership for M2

| Deliverable | Lead | Reviewers |
| --- | --- | --- |
| Fagan inspection setup + defect log | Fatima Sultan | Ahmed, Abdul |
| Static analysis triage | Abdul Hakeem | Fatima, Ahmed |
| Black-box / white-box design + RTM update | Chaudhry Ahmed Ali Khan | Fatima, Abdul |
| SCM / branching / CI evidence | Fatima Sultan | Ahmed, Abdul |

## 1. Fagan-style inspection

| Role | Person |
| --- | --- |
| Moderator | Fatima Sultan |
| Author (booking module) | Fatima Sultan |
| Reader | Chaudhry Ahmed Ali Khan |
| Recorder | Abdul Hakeem |
| External inspector | ________________ from team ________________ |

**Artifact inspected:** `BookingService.java`, `SeatInventory.java`, `schema.sql`  
**Date:** ________________ (day we actually sit with the other team)  
**Entry criteria:** code compiles; `mvn test` is green  
**Process:** overview → individual preparation → inspection meeting → defect log → rework → follow-up

### Defect log

These findings came from reviewing this codebase. We fixed or accepted them. Add any new findings from the external inspector in the same table.

| Id | Location | Severity | Finding | Disposition |
| --- | --- | --- | --- | --- |
| D1 | `Money` static init | Major | `ZERO` was built before `ROUNDING` was set → NPE on newer JDKs | Fixed. Constants declared first. Covered by `moneyRoundsAndRejectsGarbage` |
| D2 | `SimpleEventPublisher` | Major | `addIfAbsent` called through a plain `List` → project did not compile | Fixed. Field type is `CopyOnWriteArrayList` |
| D3 | Hold then save | Major | Failed save could leave seats held in memory with no booking row | Fixed. `BookingService.hold` releases inventory when persistence throws |
| D4 | Confirm then save | Major | Failed confirm write could leave seats BOOKED in memory while row stayed HELD | Fixed. `undoConfirm` puts seats back to HELD |
| D5 | `schema.sql` / booking seats | Minor | Seat status and booking row could commit separately | Fixed. `JdbcBookingRepository.saveHold` / `update` use one transaction |

CAPA: D1–D5 are closed in this tree. New rows from the external inspector stay open until a named person fixes them and the moderator checks the fix.

## 2. Static analysis

Configured in `pom.xml`:

- Checkstyle — `config/checkstyle/checkstyle.xml` (error severity fails `mvn verify`)
- PMD — `config/pmd/ruleset.xml` (report; does not fail the build by itself)
- SpotBugs — `config/spotbugs/exclude.xml`

Commands we run:

```bash
mvn -B verify
mvn -B spotbugs:spotbugs
```

Reports: `target/checkstyle-result.xml`, `target/pmd.xml`, `target/spotbugsXml.xml`.

**On this M2 build:** Checkstyle reported **0 violations**. JaCoCo line coverage ≈ **89.7%**. All 15 tests passed.

**Triage rule:** Checkstyle errors are fixed before merge. PMD / SpotBugs items are fixed, or written here as a justified false positive.

Justified PMD exclusions (`config/pmd/ruleset.xml`):

- `CloseResource` on `Database` and executor services — closed in `SeatWiseApp.close()` / after demo race; JDBC statements use try-with-resources.
- `NullAssignment` where a confirmed/cancelled booking clears `expiresAt` — null means “no deadline”.
- `AvoidUsingVolatile` on `Event.status` and `MutableClock.instant` — that is the publication strategy for those fields; inventory/bookings use locks.

SpotBugs `EI_EXPOSE_REP` on immutable lists/records is excluded because we copy with `List.copyOf` at construction. Re-check if a new method starts returning a live mutable map.

## 3. V&V plan (IEEE 1012)

| | Verification | Validation |
| --- | --- | --- |
| Question | Did we build the module the way the design says? | Did we build the ticketing behaviour users need? |
| M2 evidence | Inspection, Checkstyle, unit tests on state and pricing | Demo: hold, pay, decline, expire, race |
| M3 evidence | JaCoCo, basis-path tests, CI on push | Console walkthrough + restart test |
| Owner | Module owner reviews their package | Team lead runs `--demo` before submission |
| Timing | Before the milestone tag | At the demo |

## 4. Black-box design

### SW-FR-01 — seat hold equivalence partitions

| Partition | Input | Expected |
| --- | --- | --- |
| Valid new hold | Published event, 1–10 distinct available seats, real email | Status HELD; seats HELD |
| Invalid email | `not-an-email` | `ValidationException` |
| Too many seats | 11 or more | `ValidationException` |
| Seat already held | Same seat, second customer | `SeatUnavailableException`; first hold unchanged |
| Event not on sale | Draft or cancelled event | `ValidationException` |
| Unknown seat | Id not in the map | `EntityNotFoundException` |

### SW-FR-02 — hold duration boundaries

Hold length in tests is 10 minutes. For `isHoldExpired(now)`:

| Boundary | `now` vs `expiresAt` | Expected |
| --- | --- | --- |
| Just before | `expiresAt` − 1 s | Still HELD; expire count 0 |
| On the boundary | `now == expiresAt` | EXPIRED (`!now.isBefore(expiresAt)`) |
| Just after | `expiresAt` + 1 s | EXPIRED |

### SW-FR-03 — payment partitions

| Partition | Instrument | Expected |
| --- | --- | --- |
| Success | `4242424242424242` | CONFIRMED; payment SUCCESS |
| Decline token | contains `FAIL` | Payment FAILED; booking CANCELLED; seat AVAILABLE |
| Decline suffix | ends with `0000` | Same as decline |
| Amount mismatch | charge ≠ booking total | `ValidationException`; no confirm |

Linked tests: `BookingFlowTest`, `DomainRulesTest` (see RTM below).

## 5. White-box design

### `BookingService.expireHolds`

```text
start
  now = clock
  for each HELD booking
      if expired AND expireOne returns true
          count++
  return count
```

Cyclomatic complexity ≈ 4 (method + loop + condition + `&&`).

Basis paths:

1. No held bookings → return 0  
2. Held booking, deadline not reached → return 0  
3. Held booking, deadline reached, expire succeeds → return 1  
4. Looks expired, but under the lock it was already confirmed → `expireOne` false → count stays 0  

### `SeatInventory.hold`

Basis paths: unknown seat; at least one seat not AVAILABLE (nothing changes); all AVAILABLE (all become HELD). The concurrent race test hits the “not available” path from several threads.

## 6. SCM evidence

| Practice | Where to show it |
| --- | --- |
| Branching | `main`, `develop`, `feature/*` described in README |
| Quality gate | `mvn verify` before merge; CI runs the same command (`.github/workflows/ci.yml`) |
| Baseline | Tag `m2-baseline` on the commit we submit |
| History | `git log --oneline` shows more than one person; do not squash red-green test commits |

## 7. RTM update for the M2 build

| Id | Status in this build | Test / evidence |
| --- | --- | --- |
| SW-FR-01 | Implemented | `eightThreadsRacingForOneSeatProduceExactlyOneWinner` |
| SW-FR-02 | Implemented | `secondCustomerCannotTakeAHeldSeatAndExpiredHoldFreesIt` |
| SW-FR-03 | Implemented | `declinedPaymentReleasesTheHold`, `gatewayOutageIsRecordedAndHoldIsRolledBack` |
| SW-FR-04 | Implemented | `sqliteRoundTripSurvivesRestart` |
| SW-FR-05 | Implemented | `moneyRoundsAndRejectsGarbage`, `eventBuilderValidatesAndSeatMapIsPriced` |
| SW-FR-06 | Implemented | `bookingStateMachineRejectsIllegalMoves` |
| SW-QR-02 | Implemented | Async notifications in demo + publisher design |
| SW-QR-05 | Gate on | JaCoCo rule in `pom.xml` (~89.7% lines on this run) |
| SW-QR-06 | Implemented | CI workflow targets supported JDKs |

## 8. Risk update after M2

| Id | Change | Residual risk |
| --- | --- | --- |
| R1 | Race test passes with exactly one winner | A later change that locks per seat instead of per event could break multi-seat holds — keep the race test |
| R2 | Expire re-reads under the booking lock | Low if confirm and expire both keep using `withBookingLock` |
| R4 | Console excluded from coverage gate | Business packages still have to carry the 60% |
| R5 | Docs tied to same requirement ids | Medium — keep IDs aligned when we edit tests |
| R6 | Still needs a real external inspector | **High until the paired team signs the inspection log** |

## 9. What to upload (SE431)

- This quality / inspection document (Word or PDF as Teams asks)
- AI Usage Log (updated for M2)
- Hosted repo link
- Be ready to open Checkstyle result and the CI workflow in the viva

Prepared by Chaudhry Ahmed Ali Khan, Fatima Sultan, and Abdul Hakeem.  
Team Lead: Fatima Sultan.
