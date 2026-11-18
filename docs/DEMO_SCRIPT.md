# What to show the instructor

Do these in order. Each item is one thing you can open or run.

Team: Chaudhry Ahmed Ali Khan (catalog), Fatima Sultan (booking / lead), Abdul Hakeem (payments + notifications).

## Before the room

1. Install JDK 17 or newer and Maven.
2. In this folder run `mvn clean verify`. It must finish with `BUILD SUCCESS`.
3. Run `mvn -q package`.
4. Push the repository and confirm GitHub Actions is green at least twice.
5. Export the markdown in `docs/` to PDF if the Teams assignment asks for PDF.

## Milestone 1 (Week 6) — proposal, not the finished demo

SE423, in this order:

1. Problem and users: `docs/M1/SE423_Proposal.md`, first two sections.
2. Table that maps every Section 3.1 requirement to this project.
3. Module table with a real name next to each module.
4. Class diagram in that same file.
5. SOLID table, including why each principle applies.
6. Pattern table, and for each pattern the alternative you rejected.
7. README setup section, and `git branch` showing `main` and `develop`.

SE431, in this order:

1. ISO 25010 table tied to SeatWise requirement ids.
2. RTM v1.
3. IEEE 730 section list.
4. Cost of quality table.
5. Short note on 12207, 29110 and CMMI at a three-person scale.
6. Risk register with at least five risks, each with probability, impact, mitigation and owner.
7. `docs/AI_USAGE_LOG.md`.

## Milestone 2 (Week 10) — working core

SE423:

1. `mvn test` green.
2. `java -jar target/seatwise.jar --demo` and point at HELD, CONFIRMED, PAYMENT FAILED, EXPIRED, and `RACE winners=1`.
3. Open `Event.java` and show the Builder checks.
4. Open `HeldState.java` and show which moves are legal.
5. Open `SeatWiseApp.wire` and show the notification listener subscribed from outside the booking package.
6. Open `PaymentGateway.java` and the test that injects `DecliningPaymentGateway`.
7. Open `SeatInventory` and `BookingService` and explain the two locks and the lock order.
8. Show `git log` with separate commits for a failing test and the fix. Do not squash them.

SE431:

1. Inspection table and defect log in `docs/M2/SE431_Inspection_Tests_SCM.md`. The external inspector must be a student from the paired team, present or named.
2. `target/checkstyle-result.xml` after `mvn verify`, and say which findings were fixed.
3. V&V table: verification versus validation.
4. Equivalence partitions and boundary table for holds and payments.
5. Control-flow note and basis paths for `expireHolds`.
6. CI file and the branching rules in the README.
7. Updated RTM and risk rows at the bottom of the M2 SQA file.

## Milestone 3 (Week 13) — final demo and Q&A

SE423, live, on the jar you submitted:

1. `java -jar target/seatwise.jar` and do this by hand: list events, show the seat map, hold `VIP-A-1`, pay with a normal card reference, show the booking by email.
2. Hold another seat and pay with a reference ending in `0000`. Show the error and show the seat available again.
3. Run `--demo` for expiry and the race.
4. Stop the program, start it again, and show the confirmed booking is still in the database.
5. Open JaCoCo `target/site/jacoco/index.html` and show the line coverage at or above 60%.
6. Open `docs/M3/SE423_Final.md` and walk through the three refactors.
7. Open the Actions tab and show more than one green pipeline run.
8. Each person explains only the module they own, including one test and one design choice.

SE431:

1. Final SQAP sections in `docs/M3/SE431_Final_SQA.md`, checked against the system you just ran.
2. Surefire summary: tests run, failures 0. Mention any earlier failure and the fix.
3. GQM table with numbers copied from that run, not guessed.
4. Risk register from M1 through M3, including residual risk.
5. CAPA table showing D1–D5 closed, plus any inspection items.
6. Final RTM.
7. AI usage log.
8. Each person answers for the quality task they led.

## Marks the documents cannot earn for you

The guideline scores a live explanation. If you cannot say why the inventory lock exists, that part of the individual 20% is at risk even when the build is green. The external Fagan inspector cannot be a member of your own team. Peer evaluation is submitted separately by every student.
