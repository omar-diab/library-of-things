<div align="center">

# Library of Things

**Borrow what you need. Lend what you own. Build trust with your neighbours.**

A lending engine for communities (apartment buildings, dorms, neighbourhoods) where members
share everyday items such as tools, camping gear, electronics and kitchen equipment, instead of
everyone buying their own.

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)
![JUnit 5](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5&logoColor=white)
![JaCoCo](https://img.shields.io/badge/coverage-JaCoCo-4B32C3)

</div>

---

## Why

Most people own a drill they use twice a year. Lending it to a neighbour makes sense, but informal
lending is risky for the owner: items come back late, come back damaged, or never come back, and
there is no record of who has what.

**Library of Things** makes lending safe with three simple ideas:

| Idea | How it protects the owner |
| --- | --- |
| **Deposits** | The borrower pays a deposit set by the owner. Late fees are taken from it; damaged items keep it. |
| **Trust score** | Every member has a score from 0 to 100. Returning on time raises it; lateness and damage lower it. |
| **Clear rules** | Every borrow, renewal and reservation is checked against explicit rules, and a refusal always says why. |

## Features

- **Members and items**: register members, list items with a category and a deposit.
- **Borrowing** with six ordered eligibility checks and a clear refusal reason.
- **Returns** that settle the deposit, charge late fees, record damage and update trust.
- **Renewals** and **reservations** with their own rules.
- **Trust tiers** that decide how much, and for how long, a member may borrow.
- **Earn trust by lending**: owners gain points when their items come back on time, so a blocked member always has a way back.
- **Withdraw and restore** items, **search** by category, **fines** and payments.
- **Console menu** for trying everything by hand, with sample data.

## How it works

### Trust tiers

Every new member starts at **50** points.

| Tier | Score | Max active loans | Max loan length | Deposit paid |
| --- | --- | --- | --- | --- |
| 🔴 BLOCKED | 0 – 29 | 0 | – | – |
| 🟡 STANDARD | 30 – 69 | 2 | 7 days | 100% |
| 🟢 TRUSTED | 70 – 100 | 5 | 14 days | 50% |

| Event on return | Borrower | Owner |
| --- | --- | --- |
| On time, good condition | +5 | +2 |
| 1 – 7 days late | −10 | – |
| 8 or more days late | −20 | – |
| Damaged | −30 (plus any late change) | keeps the deposit |

### Late fees

| Days late | Fee |
| --- | --- |
| 1 – 7 | 5 TL per day |
| 8 or more | 35 TL + 10 TL per extra day |
| Cap | 150 TL per item |

The fee is taken from the deposit first. Anything above the deposit becomes a fine, and a member
with unpaid fines cannot borrow.

### Borrowing rules

A request is checked in this order, and the first rule that fails is the reason returned:

1. `OWN_ITEM`: you cannot borrow your own item.
2. `LOW_TRUST`: BLOCKED members cannot borrow.
3. `UNPAID_FINES`: fines must be paid first.
4. `LOAN_LIMIT_REACHED`: the tier's loan limit is reached.
5. `LOAN_TOO_LONG`: the loan is longer than the tier allows.
6. `ITEM_NOT_AVAILABLE`: the item is borrowed, withdrawn, or reserved by someone else.

The full specification is in [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md).

## Getting started

### Requirements

- Java 17 or newer
- Maven 3.9 or newer

### Build and run the tests

```bash
git clone https://github.com/omar-diab/library-of-things.git
cd library-of-things
mvn clean test
```

The coverage report is written to `target/site/jacoco/index.html`.

### Try it in the terminal

```bash
mvn -q compile
java -cp target/classes com.libraryofthings.Main
```

Choose `13` to load sample members and items, then try borrowing, returning and renewing:

```text
=== Library of Things ===
 1. Register member        2. Add item
 3. Borrow item            4. Return item
 5. Renew loan             6. Reserve item
 7. Pay fine               8. Withdraw item
 9. Restore item          10. Search available items
11. Show member's loans   12. Show member
13. Load sample data       0. Exit

Choose: 3
Member ID: M0002
Item ID: I0001
Start date (YYYY-MM-DD): 2026-11-02
Loan length in days: 7
Borrowed Cordless drill. Due on 2026-11-09. Deposit held: 200 TL
```

## Using the engine in code

```java
LendingService library = new LendingService();

library.registerMember("M0001", "Omar");
library.registerMember("M0002", "Sara");
library.addItem("I0001", "Cordless drill", Category.TOOLS, "M0001", 200);

Loan loan = library.borrow("M0002", "I0001", LocalDate.of(2026, 11, 2), 7);
ReturnResult result = library.returnItem("I0001", LocalDate.of(2026, 11, 10), Condition.GOOD);

result.getLateFee();         // 5 TL (one day late)
result.getDepositRefunded(); // 195 TL
```

A refused request throws an exception that carries the reason:

```java
try {
    library.borrow("M0001", "I0001", LocalDate.now(), 7);
} catch (BorrowNotAllowedException e) {
    e.getReason();           // OWN_ITEM
}
```

## Project structure

```text
src/main/java/com/libraryofthings/
├── model/        Member, Item, Loan, TrustTier and enums
├── rules/        FineCalculator, TrustPolicy, ReturnResult
├── service/      LendingService: every operation and rule
├── validation/   input format checks
├── exception/    exceptions with refusal reasons
├── console/      text menu
└── Main.java     entry point

src/test/java/com/libraryofthings/   JUnit 5 tests for every package
docs/REQUIREMENTS.md                 business rules and functional requirements
```

## Testing

The test suite is designed from the requirements, using:

- **Equivalence partitioning**: valid and invalid IDs, tiers, item states.
- **Boundary value analysis**: every limit is tested at, below and above the boundary (trust 29/30, fee days 7/8, fee cap 18/19, loan limits, deposits).
- **Decision tables**: borrowing (6 conditions), renewal (3 conditions), trust changes and deposit settlement.
- **Negative testing**: invalid input is rejected, and a refused request changes nothing.
- **Regression testing**: every defect fix starts with a failing test that stays in the suite.

JUnit 5 features used: `@ParameterizedTest` with `@CsvSource`, `@ValueSource` and `@NullAndEmptySource`, `@Nested`, `@DisplayName`, `@BeforeEach` and `assertThrows`.

## Roadmap

- [x] Core lending engine and business rules
- [x] Console menu
- [ ] REST API
- [ ] Persistent storage
- [ ] Mobile app
- [ ] Notifications for due dates and reservations

## Author

**Omar Diab** · [@omar-diab](https://github.com/omar-diab)
