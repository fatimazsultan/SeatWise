#!/usr/bin/env python3
"""Generate Milestone 2 Word documents (human student style)."""

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

HEADER_BG = "2C3E50"
ALT_BG = "F5F5F5"

TEAM = [
    ["Member 1", "Chaudhry Ahmed Ali Khan", "2023167", "u2023167@giki.edu.pk"],
    ["Member 2 (Team Lead)", "Fatima Sultan", "2023206", "u2023206@giki.edu.pk"],
    ["Member 3", "Abdul Hakeem", "2023007", "u2023007@giki.edu.pk"],
]


def shade(cell, color):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), color)
    shd.set(qn("w:val"), "clear")
    tc_pr.append(shd)


def set_run_font(run, name="Times New Roman", size=12, bold=False, italic=False, white=False):
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    run.font.size = Pt(size)
    run.bold = bold
    run.italic = italic
    if white:
        run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)


def add_page_number(paragraph):
    run1 = paragraph.add_run("Page ")
    set_run_font(run1, size=10)
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run2 = paragraph.add_run()
    run2._r.append(fld_begin)
    run2._r.append(instr)
    run2._r.append(fld_end)
    set_run_font(run2, size=10)
    run3 = paragraph.add_run(" of ")
    set_run_font(run3, size=10)
    fld_begin2 = OxmlElement("w:fldChar")
    fld_begin2.set(qn("w:fldCharType"), "begin")
    instr2 = OxmlElement("w:instrText")
    instr2.set(qn("xml:space"), "preserve")
    instr2.text = " NUMPAGES "
    fld_end2 = OxmlElement("w:fldChar")
    fld_end2.set(qn("w:fldCharType"), "end")
    run4 = paragraph.add_run()
    run4._r.append(fld_begin2)
    run4._r.append(instr2)
    run4._r.append(fld_end2)
    set_run_font(run4, size=10)


def set_cell_text(cell, text, bold=False, size=9, fill=None):
    cell.text = ""
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(1)
    p.paragraph_format.space_after = Pt(1)
    run = p.add_run(text)
    white = fill == HEADER_BG
    set_run_font(run, size=size, bold=bold or white, white=white)
    if fill:
        shade(cell, fill)


def add_table(doc, headers, rows):
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    table.style = "Table Grid"
    for i, h in enumerate(headers):
        set_cell_text(table.rows[0].cells[i], h, bold=True, size=9, fill=HEADER_BG)
    for r_i, row in enumerate(rows):
        fill = ALT_BG if r_i % 2 else None
        for c_i, val in enumerate(row):
            set_cell_text(table.rows[r_i + 1].cells[c_i], val, size=9, fill=fill)
    doc.add_paragraph()


def setup_doc(lines):
    doc = Document()
    for section in doc.sections:
        section.top_margin = Cm(2.2)
        section.bottom_margin = Cm(2.2)
        section.left_margin = Cm(2.4)
        section.right_margin = Cm(2.4)
        footer = section.footer
        footer.is_linked_to_previous = False
        p = footer.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        add_page_number(p)
    for i, line in enumerate(lines):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        run = p.add_run(line)
        set_run_font(run, size=13 if i < 3 else 12, bold=True)
    return doc


def h1(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(12)
    p.paragraph_format.space_after = Pt(4)
    set_run_font(p.add_run(text), size=13, bold=True)


def h2(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after = Pt(3)
    set_run_font(p.add_run(text), size=12, bold=True)


def body(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(5)
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.SINGLE
    set_run_font(p.add_run(text), size=11)


def bullet(doc, text):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(2)
    set_run_font(p.add_run(text), size=11)


def build_se423():
    doc = setup_doc([
        "GHULAM ISHAQ KHAN INSTITUTE",
        "OF ENGINEERING SCIENCES AND TECHNOLOGY",
        "Faculty of Computer Science and Engineering",
        "",
        "SE423 — Software Construction and Development",
        "Milestone 2 Construction Report",
        "SeatWise — Event Ticketing and Seat Reservation System",
    ])
    body(doc, "Team Lead: Fatima Sultan (2023206)")
    body(doc, "Build evidence on this tree: mvn clean verify → BUILD SUCCESS; 15 tests, 0 failures; JaCoCo ≈ 89.7% lines; Checkstyle 0 violations.")

    h1(doc, "Team roster")
    add_table(doc, ["Role", "Name", "Reg. No.", "Email"], TEAM)

    body(
        doc,
        "This is our Milestone 2 write-up for SE423. At M1 we proposed the design. "
        "Now the core actually runs: hold, pay, decline, expire, and a race for one seat.",
    )

    h1(doc, "1. What works in this build")
    body(doc, "Commands we use before demo:")
    body(doc, "mvn clean verify")
    body(doc, "mvn -q package && java -jar target/seatwise.jar --demo")
    add_table(
        doc,
        ["Outcome", "What you see"],
        [
            ["Hold + pay", "Booking ends as CONFIRMED"],
            ["Bad card (FAIL / 0000)", "PAYMENT FAILED; seat back to AVAILABLE"],
            ["Timed hold", "EXPIRED after the clock advances"],
            ["8 threads, one seat", "RACE winners=1 losers=7"],
        ],
    )

    h1(doc, "2. Modules that are implemented")
    add_table(
        doc,
        ["Module", "Main classes", "Owner"],
        [
            ["Event Catalog", "Event.Builder, SeatMapGenerator, PriceCalculator, EventService", "Chaudhry Ahmed Ali Khan"],
            ["Booking & Inventory", "Booking + states, SeatInventory, BookingService, HoldExpiryScheduler", "Fatima Sultan (Lead)"],
            ["Payments", "PaymentGateway, ScriptedPaymentGateway, PaymentService", "Abdul Hakeem"],
            ["Notifications", "NotificationService (listens to booking events)", "Abdul Hakeem"],
            ["Checkout", "CheckoutService (facade)", "Fatima Sultan (integration)"],
            ["Persistence", "schema.sql + JDBC repositories", "Shared; Fatima integrates"],
            ["Application", "SeatWiseApp, console, --demo", "Shared"],
        ],
    )
    body(
        doc,
        "Checkout is not a fifth business module stealing work from the other four. "
        "It is the purchase facade so the console does not wire catalog + booking + payment by itself. "
        "That matches what we said in M1.",
    )

    h1(doc, "3. Patterns in the running code")
    add_table(
        doc,
        ["Pattern", "Kind", "Where", "Why it is real"],
        [
            ["Builder", "Creational", "Event.Builder", "Rejects bad sales windows and duplicate section names in one place"],
            ["State", "Behavioral", "HeldState and other states", "confirm() on a pending booking throws InvalidStateTransitionException"],
            ["Observer", "Behavioral", "BookingEventPublisher, NotificationService", "booking package does not import notification"],
            ["Strategy", "Behavioral", "PaymentGateway", "Tests inject DecliningPaymentGateway without editing PaymentService"],
            ["Repository", "Port", "*Repository interfaces", "In-memory fakes and JDBC share the same ports"],
            ["Facade", "Structural", "CheckoutService", "Quote → hold → pay → roll back in one use case"],
        ],
    )
    body(doc, "Rejected alternatives are still written in the M1 proposal (long constructors, switch(status) everywhere, booking calling notifications directly, etc.).")

    h1(doc, "4. SOLID — where to open the file")
    add_table(
        doc,
        ["Principle", "What to show"],
        [
            ["SRP", "Pricing is in PriceCalculator, not inside BookingService"],
            ["OCP", "New status = new state class; BookingStates.of is the only status switch"],
            ["LSP", "Scripted and Declining gateways both return GatewayReceipt"],
            ["ISP", "PaymentRepository only has save/lookup — not seat queries"],
            ["DIP", "BookingService takes repository/publisher interfaces, not JDBC classes"],
        ],
    )

    h1(doc, "5. Defensive programming")
    body(
        doc,
        "Guard checks null, blank, email, ranges and simple business rules at service edges. "
        "Failures use typed exceptions: ValidationException, EntityNotFoundException, "
        "SeatUnavailableException, InvalidStateTransitionException, PaymentFailedException, "
        "PersistenceException. The console prints errorCode() and the message. If a notification "
        "listener throws, the booking still finishes. The hold-expiry thread catches a failed "
        "sweep and keeps going.",
    )

    h1(doc, "6. Concurrency")
    bullet(doc, "SeatInventory — one ReentrantLock per event. Multi-seat hold is check-then-write under that lock.")
    bullet(doc, "BookingService — one lock per booking id so confirm and expire cannot both win.")
    bullet(doc, "Lock order: booking lock, then inventory lock. Inventory never calls back into booking.")
    bullet(doc, "AtomicLong for hold attempts / conflicts; Money is immutable.")
    bullet(doc, "Listeners in CopyOnWriteArrayList; AsyncEventPublisher sends notifications off the booking thread.")
    bullet(doc, "If SQLite fails while saving a hold, inventory is released. If confirm fails to persist, seats go BOOKED → HELD.")

    h1(doc, "7. Tests we rely on for M2")
    add_table(
        doc,
        ["Requirement", "Test method"],
        [
            ["SW-FR-01 (no double booking)", "eightThreadsRacingForOneSeatProduceExactlyOneWinner"],
            ["SW-FR-02 (expiry)", "secondCustomerCannotTakeAHeldSeatAndExpiredHoldFreesIt"],
            ["SW-FR-03 (decline)", "declinedPaymentReleasesTheHold"],
            ["SW-FR-04 (restart)", "sqliteRoundTripSurvivesRestart"],
            ["SW-FR-05 (money)", "moneyRoundsAndRejectsGarbage / eventBuilderValidatesAndSeatMapIsPriced"],
            ["SW-FR-06 (illegal transition)", "bookingStateMachineRejectsIllegalMoves"],
        ],
    )
    body(doc, "For the live demo we also open Event.java (Builder), HeldState.java, SeatWiseApp.wire (observer wiring), PaymentGateway + declining test, SeatInventory + BookingService (locks), and git log with a failing-test commit kept separate from the fix.")

    h1(doc, "8. Who explains what in Q&A")
    add_table(
        doc,
        ["Person", "Be ready to open and explain"],
        [
            ["Chaudhry Ahmed Ali Khan", "Event.java, EventService.java, JdbcEventRepository.java"],
            ["Fatima Sultan", "BookingService.java, SeatInventory.java, HoldExpiryScheduler.java, CheckoutService.java"],
            ["Abdul Hakeem", "PaymentService.java, NotificationService.java, gateway strategy / decline path"],
        ],
    )

    h1(doc, "9. Scope note")
    body(
        doc,
        "M2 is the working core plus design evidence. Final polish, longer live console "
        "walkthrough scoring, and the finished SQAP/GQM package belong to M3. We are not "
        "claiming those here.",
    )
    body(doc, "Prepared by Chaudhry Ahmed Ali Khan, Fatima Sultan, and Abdul Hakeem. Team Lead: Fatima Sultan.")

    out = "docs/M2/SE423_M2_Construction.docx"
    doc.save(out)
    print("wrote", out)


def build_se431():
    doc = setup_doc([
        "GHULAM ISHAQ KHAN INSTITUTE",
        "OF ENGINEERING SCIENCES AND TECHNOLOGY",
        "Faculty of Computer Science and Engineering",
        "",
        "SE431 — Software Quality Assurance",
        "Milestone 2 — Inspection, Test Design and SCM",
        "SeatWise — Event Ticketing and Seat Reservation System",
    ])
    body(doc, "Team Lead: Fatima Sultan (2023206)")
    body(
        doc,
        "This is our SE431 Milestone 2 package: Fagan-style inspection, static analysis, "
        "V&V, black-box and white-box test design, SCM evidence, updated RTM, and risk notes "
        "after this build.",
    )
    body(
        doc,
        "IMPORTANT: fill in the external inspector name and the inspection date before upload. "
        "The course wants one inspector from another team — not someone from our own group.",
    )

    h1(doc, "Team roster")
    add_table(doc, ["Role", "Name", "Reg. No.", "Email"], TEAM)

    h1(doc, "0. Quality ownership for M2")
    add_table(
        doc,
        ["Deliverable", "Lead", "Reviewers"],
        [
            ["Fagan inspection setup + defect log", "Fatima Sultan", "Ahmed, Abdul"],
            ["Static analysis triage", "Abdul Hakeem", "Fatima, Ahmed"],
            ["Black-box / white-box design + RTM update", "Chaudhry Ahmed Ali Khan", "Fatima, Abdul"],
            ["SCM / branching / CI evidence", "Fatima Sultan", "Ahmed, Abdul"],
        ],
    )

    h1(doc, "1. Fagan-style inspection")
    add_table(
        doc,
        ["Role", "Person"],
        [
            ["Moderator", "Fatima Sultan"],
            ["Author (booking module)", "Fatima Sultan"],
            ["Reader", "Chaudhry Ahmed Ali Khan"],
            ["Recorder", "Abdul Hakeem"],
            ["External inspector", "________________ from team ________________"],
        ],
    )
    body(doc, "Artifact inspected: BookingService.java, SeatInventory.java, schema.sql")
    body(doc, "Date: ________________ (day we actually sit with the other team)")
    body(doc, "Entry criteria: code compiles; mvn test is green.")
    body(doc, "Process: overview → individual preparation → inspection meeting → defect log → rework → follow-up.")

    h2(doc, "1.1 Defect log")
    body(doc, "Findings from reviewing this codebase. Add any new findings from the external inspector in the same table.")
    add_table(
        doc,
        ["Id", "Location", "Severity", "Finding", "Disposition"],
        [
            ["D1", "Money static init", "Major", "ZERO built before ROUNDING → NPE on newer JDKs", "Fixed; constants first; covered by moneyRoundsAndRejectsGarbage"],
            ["D2", "SimpleEventPublisher", "Major", "addIfAbsent via List → did not compile", "Fixed; field is CopyOnWriteArrayList"],
            ["D3", "Hold then save", "Major", "Failed save could leave seats held with no booking row", "Fixed; hold releases inventory when persistence throws"],
            ["D4", "Confirm then save", "Major", "Failed confirm could leave seats BOOKED while row HELD", "Fixed; undoConfirm puts seats back to HELD"],
            ["D5", "schema / booking seats", "Minor", "Seat status and booking row could commit separately", "Fixed; saveHold/update use one transaction"],
        ],
    )
    body(doc, "CAPA: D1–D5 are closed in this tree. New external-inspector rows stay open until a named person fixes them and the moderator checks the fix.")

    h1(doc, "2. Static analysis")
    body(doc, "Tools: Checkstyle (fails verify on error), PMD (report), SpotBugs (exclusions file).")
    body(doc, "On this M2 build: Checkstyle 0 violations; JaCoCo ≈ 89.7% lines; 15/15 tests passed.")
    body(doc, "Triage: Checkstyle errors fixed before merge. PMD/SpotBugs fixed or justified here.")
    body(doc, "Justified PMD exclusions include CloseResource on Database/executors (closed in SeatWiseApp.close), NullAssignment clearing expiresAt (null = no deadline), and AvoidUsingVolatile on Event.status / MutableClock (publication strategy; inventory uses locks).")

    h1(doc, "3. V&V plan (IEEE 1012)")
    add_table(
        doc,
        ["", "Verification", "Validation"],
        [
            ["Question", "Did we build the module the way the design says?", "Did we build the ticketing behaviour users need?"],
            ["M2 evidence", "Inspection, Checkstyle, unit tests on state/pricing", "Demo: hold, pay, decline, expire, race"],
            ["M3 evidence", "JaCoCo, basis-path tests, CI on push", "Console walkthrough + restart test"],
            ["Owner", "Module owner reviews their package", "Team lead runs --demo before submission"],
            ["Timing", "Before the milestone tag", "At the demo"],
        ],
    )

    h1(doc, "4. Black-box design")
    h2(doc, "4.1 SW-FR-01 seat hold partitions")
    add_table(
        doc,
        ["Partition", "Input", "Expected"],
        [
            ["Valid new hold", "Published event, 1–10 available seats, real email", "HELD"],
            ["Invalid email", "not-an-email", "ValidationException"],
            ["Too many seats", "11 or more", "ValidationException"],
            ["Seat already held", "Same seat, second customer", "SeatUnavailableException"],
            ["Event not on sale", "Draft or cancelled", "ValidationException"],
            ["Unknown seat", "Id not in map", "EntityNotFoundException"],
        ],
    )
    h2(doc, "4.2 SW-FR-02 hold duration boundaries")
    add_table(
        doc,
        ["Boundary", "now vs expiresAt", "Expected"],
        [
            ["Just before", "expiresAt − 1 s", "Still HELD"],
            ["On the boundary", "now == expiresAt", "EXPIRED"],
            ["Just after", "expiresAt + 1 s", "EXPIRED"],
        ],
    )
    h2(doc, "4.3 SW-FR-03 payment partitions")
    add_table(
        doc,
        ["Partition", "Instrument", "Expected"],
        [
            ["Success", "4242424242424242", "CONFIRMED / SUCCESS"],
            ["Decline token", "contains FAIL", "FAILED; booking CANCELLED; seat AVAILABLE"],
            ["Decline suffix", "ends with 0000", "Same as decline"],
            ["Amount mismatch", "charge ≠ total", "ValidationException; no confirm"],
        ],
    )

    h1(doc, "5. White-box design")
    body(doc, "BookingService.expireHolds — cyclomatic complexity ≈ 4.")
    body(doc, "Basis paths: (1) no held bookings → 0; (2) held but not expired → 0; (3) expired and expireOne succeeds → 1; (4) looks expired but already confirmed under lock → 0.")
    body(doc, "SeatInventory.hold paths: unknown seat; at least one not AVAILABLE (nothing changes); all AVAILABLE (all HELD). Race test hits the not-available path.")

    h1(doc, "6. SCM evidence")
    add_table(
        doc,
        ["Practice", "Where to show it"],
        [
            ["Branching", "main, develop, feature/* in README"],
            ["Quality gate", "mvn verify before merge; CI runs same command"],
            ["Baseline", "Tag m2-baseline on the submitted commit"],
            ["History", "git log shows more than one person; do not squash red-green commits"],
        ],
    )

    h1(doc, "7. RTM update for the M2 build")
    add_table(
        doc,
        ["Id", "Status", "Test / evidence"],
        [
            ["SW-FR-01", "Implemented", "eightThreadsRacingForOneSeatProduceExactlyOneWinner"],
            ["SW-FR-02", "Implemented", "secondCustomerCannotTakeAHeldSeatAndExpiredHoldFreesIt"],
            ["SW-FR-03", "Implemented", "declinedPaymentReleasesTheHold (+ gateway outage rollback)"],
            ["SW-FR-04", "Implemented", "sqliteRoundTripSurvivesRestart"],
            ["SW-FR-05", "Implemented", "moneyRoundsAndRejectsGarbage / eventBuilderValidatesAndSeatMapIsPriced"],
            ["SW-FR-06", "Implemented", "bookingStateMachineRejectsIllegalMoves"],
            ["SW-QR-02", "Implemented", "Async notifications in demo + publisher design"],
            ["SW-QR-05", "Gate on", "JaCoCo in pom.xml (~89.7% lines this run)"],
            ["SW-QR-06", "Implemented", "CI workflow for supported JDKs"],
        ],
    )

    h1(doc, "8. Risk update after M2")
    add_table(
        doc,
        ["Id", "Change", "Residual risk"],
        [
            ["R1", "Race test passes with one winner", "Keep race test if locking strategy changes later"],
            ["R2", "Expire re-reads under booking lock", "Low if confirm/expire both keep withBookingLock"],
            ["R4", "Console excluded from coverage gate", "Business packages still carry 60%"],
            ["R5", "Docs tied to same requirement ids", "Medium — keep IDs aligned when editing tests"],
            ["R6", "Still needs external inspector", "High until paired team signs the inspection log"],
        ],
    )

    h1(doc, "9. Upload checklist (SE431)")
    bullet(doc, "This inspection / test-design document (Word or PDF as Teams asks)")
    bullet(doc, "AI Usage Log updated for M2")
    bullet(doc, "Hosted repository link")
    bullet(doc, "Be ready to open Checkstyle result and CI workflow in the viva")

    body(doc, "Prepared by Chaudhry Ahmed Ali Khan, Fatima Sultan, and Abdul Hakeem. Team Lead: Fatima Sultan.")

    out = "docs/M2/SE431_M2_Inspection_Tests_SCM.docx"
    doc.save(out)
    print("wrote", out)


def build_ai_log():
    doc = setup_doc([
        "AI Usage Log",
        "SeatWise — Joint SE423 / SE431 Project",
        "Milestone 2",
    ])
    body(
        doc,
        "AI tools are allowed. Marks still depend on whether we understand the work and can "
        "defend it. Significant uses are listed below.",
    )
    h1(doc, "Team")
    add_table(doc, ["Role", "Name", "Reg. No.", "Email"], TEAM)

    h1(doc, "Log entries")
    add_table(
        doc,
        ["Date", "Tool", "Purpose", "What we used it for", "What we kept", "What we changed", "How we checked"],
        [
            [
                "2026-10-06",
                "Cursor",
                "Scaffold Java system",
                "Help implement SeatWise against joint guidelines",
                "Modules, patterns, tests, CI, draft docs",
                "Assigned real owners; practised explanations",
                "mvn verify; --demo",
            ],
            [
                "2026-10-09",
                "Cursor",
                "Milestone 1 docs",
                "SE423 proposal + SE431 quality plan Word files",
                "M1 structure and rubric coverage",
                "Rewrote wording; Fatima set as lead",
                "Rubric checklist before upload",
            ],
            [
                "2026-11-10",
                "Cursor",
                "Milestone 2 package",
                "Construction report, inspection/test/SCM docs, AI log",
                "M2 evidence tables tied to real tests/demo",
                "Filled real roster; left external inspector blank for pairing",
                "mvn clean verify (15/15); --demo (CONFIRMED, FAILED, EXPIRED, RACE winners=1)",
            ],
        ],
    )
    body(doc, "Each of us must be able to explain the module and quality area we own, including AI-assisted parts.")

    out = "docs/M2/AI_Usage_Log.docx"
    doc.save(out)
    print("wrote", out)


if __name__ == "__main__":
    build_se423()
    build_se431()
    build_ai_log()
