# Library of Things – Requirements Specification

## 1. Problem and Scope

People in the same building, dorm or neighbourhood often buy items they rarely use
(a drill, a ladder, a tent, a projector). **Library of Things** lets community members lend
their own items to each other. To keep lending safe, the system holds a **deposit**, charges
**late fees**, and keeps a **trust score** for every member that controls how much they can borrow.

This repository is the **core lending engine**: a Java application with no GUI and no database
(data is kept in memory). All dates are passed in as parameters, so any day can be tested.

**Out of scope (future product work):** mobile app, user accounts and login, payments,
database, notifications, more than one reservation per item.

## 2. Data and Validation Rules (BR8)

| Field | Rule | Valid example | Invalid examples |
|---|---|---|---|
| Member ID | `M` followed by exactly 4 digits | `M0001` | `M001`, `M00001`, `m0001`, `X0001`, `null` |
| Item ID | `I` followed by exactly 4 digits | `I0001` | `I01`, `i0001`, `M0001`, `null` |
| Name (member / item) | not null, not blank | `Drill` | `""`, `"   "`, `null` |
| Category | one of `TOOLS`, `OUTDOOR`, `ELECTRONICS`, `KITCHEN`, `OTHER` | `TOOLS` | `null` |
| Deposit (TL) | whole number from 0 to 1000 | `0`, `200`, `1000` | `-1`, `1001` |
| Loan length (days) | at least 1 | `1`, `7` | `0`, `-3` |
| Dates | not null | `2026-11-18` | `null` |
| Payment amount (TL) | > 0 and ≤ member's unpaid fines | | `0`, `-5`, more than owed |

Invalid input throws `InvalidInputException`.
An unknown member ID or item ID throws `NotFoundException`.

## 3. Trust Score and Tiers (BR1, BR2)

Every member has a **trust score from 0 to 100**. A new member starts at **50**.
The score decides the member's tier:

| Tier | Score range | Max active loans | Max loan length | Deposit paid |
|---|---|---|---|---|
| BLOCKED | 0 – 29 | 0 (cannot borrow) | – | – |
| STANDARD | 30 – 69 | 2 | 7 days | 100% of the item's deposit |
| TRUSTED | 70 – 100 | 5 | 14 days | 50% of the item's deposit (rounded down to whole TL) |

**BR2 – Trust score changes when an item is returned:**

| Event | Change |
|---|---|
| Returned on time (0 days late) and in good condition | +5 |
| Returned 1 – 7 days late | −10 |
| Returned 8 or more days late | −20 |
| Returned damaged | −30 (in addition to any late change) |

The score never goes above 100 or below 0.

## 4. Late Fee Rule (BR3)

| Days late (d) | Fee |
|---|---|
| d = 0 | 0 TL |
| 1 ≤ d ≤ 7 | 5 × d TL |
| d ≥ 8 | 35 + 10 × (d − 7) TL, but never more than **150 TL** |
| d < 0 | invalid → `InvalidInputException` |

Key values: d=7 → 35 TL, d=8 → 45 TL, d=18 → 145 TL, d=19 → 150 TL (capped).

## 5. Functional Requirements

### FR1 – Register a member
Input: member ID, name. The ID must be valid and unique (duplicate → `InvalidInputException`).
A new member has trust score 50, 0 TL unpaid fines and no loans.

### FR2 – Add an item
Input: item ID, name, category, owner's member ID, deposit.
The item ID must be valid and unique; the owner must be a registered member.
A new item is `AVAILABLE` and not reserved.

### FR3 – Borrow an item (BR4)
Input: member ID, item ID, start date, loan length in days.
The request is checked in this **fixed order**; the first rule that fails is reported in
`BorrowNotAllowedException` with its reason:

| Order | Condition that blocks borrowing | Reason |
|---|---|---|
| 1 | The member is the item's owner | `OWN_ITEM` |
| 2 | The member's tier is BLOCKED (score < 30) | `LOW_TRUST` |
| 3 | The member has unpaid fines > 0 TL | `UNPAID_FINES` |
| 4 | The member already has the maximum number of active loans for their tier | `LOAN_LIMIT_REACHED` |
| 5 | The loan length is longer than the tier's maximum | `LOAN_TOO_LONG` |
| 6 | The item is borrowed, withdrawn, or reserved by a **different** member | `ITEM_NOT_AVAILABLE` |

If all checks pass:
- a loan is created with due date = start date + loan length;
- the deposit held = the item's deposit (STANDARD) or half of it rounded down (TRUSTED);
- the item becomes `BORROWED`; if this member had reserved it, the reservation is cleared.

### FR4 – Return an item (BR5)
Input: item ID, return date, condition (`GOOD` or `DAMAGED`).
- The item must currently be borrowed, and the return date cannot be before the start date
  (otherwise `InvalidInputException`).
- Days late = return date − due date (0 if returned on or before the due date).
- Late fee is calculated with BR3.
- **Condition GOOD:** the late fee is taken from the held deposit first, and that part goes to the owner.
  Refund to the borrower = held deposit − late fee (not below 0).
  If the late fee is larger than the deposit, the difference is added to the borrower's unpaid fines.
  The item becomes `AVAILABLE` (it stays reserved if someone reserved it).
- **Condition DAMAGED:** the whole held deposit goes to the owner (no refund), the full late fee
  is added to the borrower's unpaid fines, and the item becomes `WITHDRAWN` until the owner restores it.
- The trust score is updated with BR2.
- The method returns a result with: days late, late fee, deposit refunded, deposit paid to the owner, fine added, trust change.

### FR5 – Renew a loan (BR6)
Input: item ID, renewal date. The item must currently be borrowed, and the renewal date cannot be
before the loan's start date (otherwise `InvalidInputException`).
Checked in this fixed order; the first failure is reported in `RenewalNotAllowedException`:

| Order | Condition that blocks renewal | Reason |
|---|---|---|
| 1 | The renewal date is **after** the due date (renewing *on* the due date is allowed) | `OVERDUE` |
| 2 | The loan has already been renewed once (maximum 1 renewal) | `RENEWAL_LIMIT_REACHED` |
| 3 | The item is reserved by another member | `RESERVED_BY_OTHER` |

If all checks pass: new due date = old due date + original loan length; renewal count becomes 1.

### FR6 – Pay a fine
Input: member ID, amount. The amount must be > 0 and ≤ unpaid fines.
Unpaid fines decrease by the amount.

### FR7 – Reserve an item (BR7)
Input: member ID, item ID. All violations throw `InvalidInputException`:
- only a `BORROWED` item can be reserved (an available item should simply be borrowed);
- the item must not already be reserved;
- the current borrower and the item's owner cannot reserve it.

### FR8 – Withdraw and restore an item
Input: member ID, item ID.
- Only the owner can withdraw or restore their item (otherwise `InvalidInputException`).
- A borrowed item cannot be withdrawn.
- Withdraw: `AVAILABLE` → `WITHDRAWN`. Restore: `WITHDRAWN` → `AVAILABLE`.

### FR9 – Search available items
Input: member ID, category. Output: items in that category that are `AVAILABLE`,
not reserved by someone else, and not owned by the member searching.

### FR10 – List a member's active loans
Input: member ID. Output: the member's loans that have not been returned.

## 6. Design Overview

| Class | Responsibility |
|---|---|
| `Member` | ID, name, trust score, unpaid fines |
| `TrustTier` (enum) | BLOCKED / STANDARD / TRUSTED with their limits; finds the tier for a score |
| `Item` | ID, name, category, owner, deposit, status, reservation |
| `Category`, `ItemStatus`, `Condition` (enums) | fixed lists of values |
| `Loan` | item, borrower, start date, due date, loan length, deposit held, renewals, return date |
| `ReturnResult` | days late, late fee, deposit refunded, deposit to owner, fine added, trust change |
| `Validator` | format checks for IDs, names, deposits, dates |
| `FineCalculator` | late fee rule (BR3) |
| `TrustPolicy` | trust score changes (BR2) |
| `LendingService` | FR1–FR10, applies all business rules |
| Exceptions | `InvalidInputException`, `NotFoundException`, `BorrowNotAllowedException`, `RenewalNotAllowedException` |
